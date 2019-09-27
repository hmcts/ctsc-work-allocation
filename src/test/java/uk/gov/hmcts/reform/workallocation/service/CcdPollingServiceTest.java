package uk.gov.hmcts.reform.workallocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.workallocation.ccd.CcdClient;
import uk.gov.hmcts.reform.workallocation.idam.IdamService;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.queue.DeadQueueConsumer;
import uk.gov.hmcts.reform.workallocation.queue.QueueConsumer;
import uk.gov.hmcts.reform.workallocation.queue.QueueProducer;
import uk.gov.hmcts.reform.workallocation.services.CcdPollingService;
import uk.gov.hmcts.reform.workallocation.services.LastRunTimeService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CcdPollingServiceTest {

    @InjectMocks
    private CcdPollingService ccdPollingService;

    @Mock
    private IdamService idamService;

    @Mock
    private CcdClient ccdClient;

    @Mock
    private LastRunTimeService lastRunTimeService;

    @Mock
    private QueueProducer<Task> queueProducer;

    @Mock
    private QueueConsumer<Task> queueConsumer;

    @Mock
    private DeadQueueConsumer deadQueueConsumer;

    @Before
    public void setup() throws ServiceBusException, InterruptedException, IOException {
        MockitoAnnotations.initMocks(this);
        ccdPollingService = new CcdPollingService(idamService, ccdClient, lastRunTimeService, queueProducer,
            queueConsumer, deadQueueConsumer);
        ReflectionTestUtils.setField(ccdPollingService, "ctids", "DIVORCE");
        ReflectionTestUtils.setField(ccdPollingService, "deeplinkBaseUrl", "ccd_server_url");

        when(deadQueueConsumer.runConsumer(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(queueConsumer.runConsumer(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(ccdClient.searchCases(anyString(), anyString(), anyString(), anyString())).thenReturn(caseSearchResult());
        when(idamService.generateServiceAuthorization()).thenReturn("service_token");
        when(idamService.getIdamOauth2Token()).thenReturn("idam_token");
        when(lastRunTimeService.getMinDate()).thenReturn(LocalDateTime.of(2019, 9, 20, 12, 0, 0, 0));
    }

    @Test
    public void testPollccdEndpoint() throws ServiceBusException, InterruptedException {
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.of(LocalDateTime.of(2019, 9, 25, 12, 0, 0, 0)));

        ccdPollingService.pollCcdEndpoint();
        String query = composeQuery("2019-09-25T11:59:55");
        Task task = getTask();
        verify(ccdClient, times(1)).searchCases("idam_token", "service_token", "DIVORCE", query);
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Collections.singletonList(task)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointFirstTime() {
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.empty());

        ccdPollingService.pollCcdEndpoint();
        String query = composeQuery("2019-09-20T11:59:55");
        Task task = getTask();
        verify(ccdClient, times(1)).searchCases("idam_token", "service_token", "DIVORCE", query);
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Collections.singletonList(task)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointWhenQueueConsumerThrowsAnError() {
        CompletableFuture<Void> consumerResponse = new CompletableFuture<>();
        consumerResponse.completeExceptionally(new RuntimeException("Something went wrong"));
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.of(LocalDateTime.of(2019, 9, 25, 12, 0, 0, 0)));
        when(queueConsumer.runConsumer(any())).thenReturn(consumerResponse);
        ccdPollingService.pollCcdEndpoint();
        String query = composeQuery("2019-09-25T11:59:55");
        Task task = getTask();
        verify(ccdClient, times(1)).searchCases("idam_token", "service_token", "DIVORCE", query);
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Collections.singletonList(task)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointWhenDeadQueueConsumerThrowsAnError() {
        CompletableFuture<Void> consumerResponse = new CompletableFuture<>();
        consumerResponse.completeExceptionally(new RuntimeException("Something went wrong"));
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.of(LocalDateTime.of(2019, 9, 25, 12, 0, 0, 0)));
        when(deadQueueConsumer.runConsumer(any())).thenReturn(consumerResponse);
        ccdPollingService.pollCcdEndpoint();
        String query = composeQuery("2019-09-25T11:59:55");
        Task task = getTask();
        verify(ccdClient, times(1)).searchCases("idam_token", "service_token", "DIVORCE", query);
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Collections.singletonList(task)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    //CHECKSTYLE:OFF
    @SuppressWarnings("unchecked")
    private Map<String, Object> caseSearchResult() throws IOException {
        String json = "{\n"
            + "\"total\": 1,\n"
            + "  \"cases\": [\n"
            + "  {\n"
            + "    \"id\": 1563460551495313,\n"
            + "    \"jurisdiction\": \"DIVORCE\",\n"
            + "    \"state\": \"Submitted\",\n"
            + "    \"version\": null,\n"
            + "    \"case_type_id\": \"DIVORCE\",\n"
            + "    \"created_date\": \"2019-07-18T14:35:51.473\",\n"
            + "    \"last_modified\": \"2019-07-18T14:36:25.862\",\n"
            + "    \"security_classification\": \"PUBLIC\"\n"
            + "  }\n"
            + "]\n"
            + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }
    //CHECKSTYLE:ON

    private Task getTask() {
        return Task.builder()
            .caseTypeId("DIVORCE")
            .id("1563460551495313")
            .jurisdiction("DIVORCE")
            .state("Submitted")
            .lastModifiedDate(LocalDateTime.of(2019,7,18,14,36,25, 862000000))
            .build();
    }

    private String composeQuery(String date) {
        return "{\"query\":{\"bool\":{\"must\":[{\"range\":{\"last_modified\":{\"gte\":\""
            + date + "\"}}},{\"match\":{\"state\":{\"query\": \"Submitted AwaitingHWFDecision DARequested\","
            + "\"operator\": \"or\"}}}]}},\"size\": 500}";
    }
}
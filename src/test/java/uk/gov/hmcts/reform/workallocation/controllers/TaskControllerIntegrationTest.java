package uk.gov.hmcts.reform.workallocation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.workallocation.Application;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, webEnvironment = MOCK)

public class TaskControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mvc;

    private final String token = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjdHNjX3dvcmtfYWxsb2NhdGlvbiIsImV4cCI"
        + "6MTU3MDAyNjE3Nn0.Y7u9WqS9R_P-ckt1ccYzJ_k5nrF7B6LXZDYT6LCalsxMUfgODdLvtDfDwuINRyvT3zYE2P2-EXI9bbdqjvzfaw";

    private final String payload = "{\n" +
        "  \"id\": 1563460551495313,\n" +
        "  \"jurisdiction\": \"DIVORCE\",\n" +
        "  \"state\": \"valami\",\n" +
        "  \"case_type_id\": \"DIVORCE\",\n" +
        "  \"last_modified_date\": \"2019-07-18T14:36:25.862\"\n" +
        "}";


    @Before
    public void setUp() {
        mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    public void testAddTaskWithoutAuth() throws Exception {
        mvc.perform(post("/task")).andDo(print())

            .andExpect(status().isForbidden());
    }

    @Test
    public void testAddTaskWithAuth() throws Exception {
        mvc.perform(post("/task")
            .header("ServiceAuthorization", token)
            .content(payload)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
            .andDo(print())

            .andExpect(status().isOk());
    }
}
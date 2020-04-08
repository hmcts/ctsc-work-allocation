package uk.gov.hmcts.reform.workallocation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.reform.workallocation.exception.CaseTransformException;
import uk.gov.hmcts.reform.workallocation.services.CcdConnectorService;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Task {

    private String id;
    private String state;
    private String jurisdiction;
    private String caseTypeId;
    private LocalDateTime lastModifiedDate;

    public static Task fromCcdCase(Map<String, Object> caseData, String caseTypeId) throws CaseTransformException {
        if (CcdConnectorService.CASE_TYPE_ID_DIVORCE.equals(caseTypeId)) {
            return fromCcdDivorceCase(caseData);
        }
        if (CcdConnectorService.CASE_TYPE_ID_PROBATE.equals(caseTypeId)) {
            return fromCcdProbateCase(caseData);
        }
        if (CcdConnectorService.CASE_TYPE_ID_BULK_SCANNING.equals(caseTypeId)) {
            return fromCcdBulkScanCase(caseData);
        }
        throw new CaseTransformException("Unknown case type: " + caseTypeId);
    }

    private static Task fromCcdDivorceCase(Map<String, Object> caseData) throws CaseTransformException {
        try {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(caseData.get("last_modified").toString());
            return Task.builder()
                .id(((Long)caseData.get("id")).toString())
                .state((String) caseData.get("state"))
                .jurisdiction((String) caseData.get("jurisdiction"))
                .caseTypeId(CcdConnectorService.CASE_TYPE_ID_DIVORCE)
                .lastModifiedDate(lastModifiedDate)
                .build();
        } catch (Exception e) {
            throw new CaseTransformException("Failed to transform the case", e);
        }
    }

    private static Task fromCcdProbateCase(Map<String, Object> caseData) throws CaseTransformException {
        try {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(caseData.get("last_modified").toString());
            return Task.builder()
                .id(((Long)caseData.get("id")).toString())
                .state(getProbateState(caseData))
                .jurisdiction((String) caseData.get("jurisdiction"))
                .caseTypeId(CcdConnectorService.CASE_TYPE_ID_PROBATE)
                .lastModifiedDate(lastModifiedDate)
                .build();
        } catch (Exception e) {
            throw new CaseTransformException("Failed to transform the case", e);
        }
    }

    private static Task fromCcdBulkScanCase(Map<String, Object> caseData) throws CaseTransformException {
        try {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(caseData.get("last_modified").toString());
            return Task.builder()
                .id(((Long)caseData.get("id")).toString())
                .state(getBulkScanningState(caseData))
                .jurisdiction((String) caseData.get("jurisdiction"))
                .caseTypeId(CcdConnectorService.CASE_TYPE_ID_BULK_SCANNING)
                .lastModifiedDate(lastModifiedDate)
                .build();
        } catch (Exception e) {
            throw new CaseTransformException("Failed to transform the case", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String getProbateState(Map<String, Object> caseData) {
        String state = (String) caseData.get("state");
        Map<String, Object> caseProperties = (Map<String, Object>) caseData.get("case_data");
        if ("CaseCreated".equals(state)) {
            return "CaseCreated";
        }
        if ("CasePrinted".equals(state) && "No".equals(caseProperties.get("evidenceHandled"))) {
            return "AwaitingDocumentation";
        }
        if ("BOCaseStopped".equals(state) && caseProperties.get("evidenceHandled") != "No") {
            return "CaseStopped - N";
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static String getBulkScanningState(Map<String, Object> caseData) {
        String state = (String) caseData.get("state");
        Map<String, Object> caseProperties = (Map<String, Object>) caseData.get("case_data");
        if ("ScannedRecordReceived".equals(state)
            && "NEW_APPLICATION".equals(caseProperties.get("journeyClassification"))) {
            return "BulkScanning – NewPay";
        }
        if ("ScannedRecordReceived".equals(state)
            && "SUPPLEMENTARY_EVIDENCE".equals(caseProperties.get("journeyClassification"))) {
            return "BulkScanning – Supp";
        }
        return state;
    }
}

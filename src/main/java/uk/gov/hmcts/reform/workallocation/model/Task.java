package uk.gov.hmcts.reform.workallocation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import uk.gov.hmcts.reform.workallocation.exception.CaseTransformException;

import java.time.LocalDateTime;
import java.util.Map;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Task implements Comparable<Task> {

    @Id
    private String id;

    @NonNull
    private String state;

    @NonNull
    private String jurisdiction;

    @NonNull
    private String caseTypeId;

    @NonNull
    @Convert(converter = Jsr310JpaConverters.LocalDateTimeConverter.class)
    private LocalDateTime lastModifiedDate;

    private boolean sent;

    public static Task fromCcdDCase(Map<String, Object> caseData) throws CaseTransformException {
        try {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(caseData.get("last_modified").toString());
            return Task.builder()
                .id(((Long)caseData.get("id")).toString())
                .state((String) caseData.get("state"))
                .jurisdiction((String) caseData.get("jurisdiction"))
                .caseTypeId((String) caseData.get("case_type_id"))
                .lastModifiedDate(lastModifiedDate)
                .build();
        } catch (Exception e) {
            throw new CaseTransformException("Failed to transform the case", e);
        }
    }

    @Override
    public int compareTo(Task o) {
        return o.lastModifiedDate.compareTo(this.lastModifiedDate);
    }
}

package mx.uam.sapcyti.survey.application.service;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;

@Value
@Builder
public class SubmittedResponseView {

    AcademicTerm academicTerm;
    SurveyResponseMode mode;
    List<Long> ueaIds;
    int totalUeas;
    Instant submittedAt;

    public static SubmittedResponseView from(StudentSurveyResponse response) {
        return SubmittedResponseView.builder()
                .academicTerm(response.getAcademicTerm())
                .mode(response.getMode())
                .ueaIds(response.getUeaIds())
                .totalUeas(response.getTotalUeas())
                .submittedAt(response.getUpdatedAt())
                .build();
    }
}

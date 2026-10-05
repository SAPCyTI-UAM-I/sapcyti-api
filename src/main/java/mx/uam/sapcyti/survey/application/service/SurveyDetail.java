package mx.uam.sapcyti.survey.application.service;

import lombok.Builder;
import lombok.Value;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;

@Value
@Builder
public class SurveyDetail {

    EnrollmentSurvey survey;
    long responseCount;
    String suggestedTerm;
}

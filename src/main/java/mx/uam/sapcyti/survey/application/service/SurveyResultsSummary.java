package mx.uam.sapcyti.survey.application.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SurveyResultsSummary {
    long eligibleCount;
    long respondedCount;
    long pendingCount;
}

package mx.uam.sapcyti.survey.application.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SurveyResultsSummary {
    long eligibleCount;
    long respondedCount;
    long pendingCount;
    /** Responses with mode BLANK — they count as responded but add no UEA demand rows. */
    long blankCount;
}

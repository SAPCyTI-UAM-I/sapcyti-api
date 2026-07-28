package mx.uam.sapcyti.survey.infrastructure.adapter.in.dto;

public record SurveyResultsSummaryResponse(
        long eligibleCount, long respondedCount, long pendingCount, long blankCount) {}

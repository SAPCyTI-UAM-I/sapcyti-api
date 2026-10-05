package mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto;

import java.time.Instant;
import java.util.List;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanStatus;

public record TrimestralPlanSummaryResponse(
        Long id,
        String term,
        TrimestralPlanStatus status,
        Long surveyId,
        boolean outdated,
        Instant exportedAt,
        int groupCount,
        int blankCount) {

    public static TrimestralPlanSummaryResponse from(TrimestralPlan plan, int groupCount, int blankCount) {
        return new TrimestralPlanSummaryResponse(
                plan.getId(),
                plan.getTerm(),
                plan.getStatus(),
                plan.getSurveyId(),
                plan.isOutdated(),
                plan.getExportedAt(),
                groupCount,
                blankCount);
    }
}

package mx.uam.sapcyti.planning.infrastructure.adapter.in.dto;

import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanStatus;

public record AnnualPlanSummaryResponse(int year, AnnualPlanStatus status) {

    public static AnnualPlanSummaryResponse from(AnnualPlan plan) {
        return new AnnualPlanSummaryResponse(plan.getYear(), plan.getStatus());
    }
}

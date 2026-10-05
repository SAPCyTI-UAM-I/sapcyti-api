package mx.uam.sapcyti.planning.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.NotNull;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanStatus;

public record ChangeStatusRequest(@NotNull AnnualPlanStatus status) {}

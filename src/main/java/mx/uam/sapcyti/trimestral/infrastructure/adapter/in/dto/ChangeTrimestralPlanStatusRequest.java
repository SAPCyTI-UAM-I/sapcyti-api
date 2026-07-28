package mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.NotNull;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanStatus;

public record ChangeTrimestralPlanStatusRequest(@NotNull TrimestralPlanStatus status) {}

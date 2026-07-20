package mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.NotNull;

public record CreateTrimestralPlanRequest(@NotNull Long surveyId) {}

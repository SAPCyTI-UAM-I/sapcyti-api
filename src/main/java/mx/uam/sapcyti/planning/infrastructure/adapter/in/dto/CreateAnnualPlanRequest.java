package mx.uam.sapcyti.planning.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateAnnualPlanRequest(
        @NotNull @Min(2000) @Max(2100) Integer year) {}

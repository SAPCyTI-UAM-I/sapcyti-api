package mx.uam.sapcyti.survey.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateSurveyRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{2}[OIP]$") @Schema(example = "26O") String term,
        @NotNull Instant opensAt,
        @NotNull Instant closesAt,
        @Size(max = 500) String introMessage) {}

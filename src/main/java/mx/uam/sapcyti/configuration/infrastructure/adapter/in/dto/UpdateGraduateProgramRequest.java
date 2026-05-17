package mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HTTP request body for updating a graduate program.
 */
public record UpdateGraduateProgramRequest(
    @NotBlank(message = "Program name is required")
    @Size(max = 200)
    String name,

    @NotBlank(message = "Division is required")
    @Size(max = 100)
    String division) {
}

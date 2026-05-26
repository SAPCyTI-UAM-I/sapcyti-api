package mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HTTP request body for creating a graduate program.
 */
public record CreateGraduateProgramRequest(
    @NotBlank(message = "Program name is required")
    @Size(max = 200)
    String name,

    @NotBlank(message = "Division is required")
    @Size(max = 100)
    String division,

    @Valid
    List<InitialParameterRequest> initialParameters) {
}

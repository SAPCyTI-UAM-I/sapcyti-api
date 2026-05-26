package mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * HTTP request body for setting a configuration parameter.
 */
public record SetConfigurationParameterRequest(
    @NotBlank(message = "Parameter key is required")
    @Size(max = 100)
    @Pattern(
        regexp = "^[A-Z][A-Z0-9_]*$",
        message = "Key must be in UPPER_SNAKE_CASE format")
    String key,

    @NotBlank(message = "Parameter value is required")
    @Size(max = 500)
    String value,

    @Size(max = 500)
    String description) {
}

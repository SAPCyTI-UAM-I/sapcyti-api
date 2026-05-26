package mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto;

import java.util.List;

/**
 * API response for graduate program detail (schema: GraduateProgramResponse).
 */
public record GraduateProgramResponse(
    Long id,
    String name,
    String division,
    List<ConfigurationParameterResponse> configurationParameters) {
}

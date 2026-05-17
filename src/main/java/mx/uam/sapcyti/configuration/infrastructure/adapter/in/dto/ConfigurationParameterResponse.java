package mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto;

/**
 * API response for a configuration parameter.
 */
public record ConfigurationParameterResponse(
    String key,
    String value,
    String description) {
}

package mx.uam.sapcyti.configuration.application.command;

/**
 * Command to create or update a configuration parameter (schema: SetConfigurationParameterCommand).
 */
public record SetConfigurationParameterCommand(
    String key,
    String value,
    String description) {
}

package mx.uam.sapcyti.configuration.application.command;

/**
 * Optional initial configuration parameter when creating a program.
 */
public record InitialParameterCommand(
    String key,
    String value,
    String description) {
}

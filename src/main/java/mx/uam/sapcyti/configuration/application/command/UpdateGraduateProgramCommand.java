package mx.uam.sapcyti.configuration.application.command;

/**
 * Command to update basic graduate program data (schema: UpdateGraduateProgramCommand).
 */
public record UpdateGraduateProgramCommand(
    Long id,
    String name,
    String division) {
}

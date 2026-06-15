package mx.uam.sapcyti.academic.application.command;

/**
 * Command to register a new professor (HU-21).
 */
public record RegisterProfessorCommand(
        String employeeNumber,
        String email,
        Long graduateProgramId,
        String firstName,
        String firstLastName,
        String secondLastName) {
}

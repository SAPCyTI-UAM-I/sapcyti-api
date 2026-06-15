package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

/**
 * Read model for professor data (HU-21).
 */
public record ProfessorResponse(
        Long id,
        String employeeNumber,
        String email,
        String firstName,
        String firstLastName,
        String secondLastName,
        Long graduateProgramId,
        String generatedPassword) {
}

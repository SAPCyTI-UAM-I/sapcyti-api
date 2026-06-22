package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

/**
 * Read-model projection of a professor for tutor/advisor display (HU-19).
 */
public record ProfessorReferenceResponse(
        Long id,
        String firstName,
        String firstLastName,
        String secondLastName) {
}

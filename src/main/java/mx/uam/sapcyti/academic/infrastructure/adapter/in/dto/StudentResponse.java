package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Read model for student data (HU-15).
 */
public record StudentResponse(
        Long id,
        String enrollmentId,
        String email,
        String firstName,
        String firstLastName,
        String secondLastName,
        String nationality,
        String undergraduateDegree,
        ProgramType programType,
        LocalDate admissionDate,
        Long advisorId,
        Long graduateProgramId,
        String generatedPassword) {
}

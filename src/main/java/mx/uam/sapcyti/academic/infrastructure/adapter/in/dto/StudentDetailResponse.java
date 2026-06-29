package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Unified student detail read model with embedded program (HU-17).
 */
@Schema(name = "StudentDetailResponse", description = "Student personal data with embedded academic program.")
public record StudentDetailResponse(
        Long id,
        String enrollmentId,
        String email,
        String firstName,
        String firstLastName,
        String secondLastName,
        String nationality,
        LocalDate birthDate,
        String phone,
        String phoneExtension,
        String undergraduateDegree,
        String lastDegreeObtained,
        ProgramType programType,
        LocalDate admissionDate,
        Long advisorId,
        Long graduateProgramId,
        Long userId,
        boolean active,
        StudentProgramResponse program) {
}

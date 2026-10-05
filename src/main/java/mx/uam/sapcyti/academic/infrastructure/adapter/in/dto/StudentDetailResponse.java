package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Unified student detail read model with embedded program (HU-17 / HU-56).
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
        DegreeLevel lastDegreeObtained,
        ProgramType programType,
        LocalDate admissionDate,
        @Schema(description = "Admission trimester AA + O|I|P (e.g. 26O). Null for legacy rows.", example = "26O")
        String admissionTerm,
        Long advisorId,
        Long graduateProgramId,
        Long userId,
        boolean active,
        StudentProgramResponse program) {
}

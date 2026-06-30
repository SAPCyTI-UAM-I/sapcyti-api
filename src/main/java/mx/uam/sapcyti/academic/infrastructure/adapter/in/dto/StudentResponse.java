package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
        LocalDate birthDate,
        String phone,
        String phoneExtension,
        String undergraduateDegree,
        String lastDegreeObtained,
        ProgramType programType,
        LocalDate admissionDate,
        Long advisorId,
        Long graduateProgramId,
        @Schema(description = "Identifier of the user account linked to this student.", accessMode = Schema.AccessMode.READ_ONLY)
        Long userId,
        @Schema(description = "Whether the student account is active.", accessMode = Schema.AccessMode.READ_ONLY)
        boolean active,
        Long tutorId,
        java.util.List<Long> advisorIds,
        String lineOfKnowledge,
        String researchArea,
        @Schema(
                description = "One-time server-generated temporary password. Present only in the registration (create) "
                        + "response; null on list/get.",
                accessMode = Schema.AccessMode.READ_ONLY)
        String generatedPassword) {
}

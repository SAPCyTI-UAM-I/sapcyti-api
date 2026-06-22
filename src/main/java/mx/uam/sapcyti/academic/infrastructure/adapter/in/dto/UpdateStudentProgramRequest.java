package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;

/**
 * Request body for updating a student program (HU-20).
 */
public record UpdateStudentProgramRequest(
        @NotNull LocalDate admissionDate,
        LocalDate graduationDate,
        @Size(max = 200) String researchArea,
        @NotNull ProgramStatus status,
        @Size(max = 500) String withdrawalReason,
        Long tutorId,
        @NotNull List<Long> advisorIds) {
}

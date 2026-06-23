package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Lightweight read model for listing student programs (HU-19).
 */
@Schema(name = "StudentProgramSummary", description = "Summary of a student's program enrollment for list views.")
public record StudentProgramSummaryResponse(
        @Schema(description = "Student program identifier.", example = "100") Long id,
        @Schema(description = "Program level.", example = "MAESTRIA") ProgramType programType,
        @Schema(description = "Enrollment or matricula identifier.", example = "2123803361") String enrollmentId,
        @Schema(description = "Program lifecycle status.", example = "ACTIVO") ProgramStatus status,
        @Schema(description = "Assigned tutor professor id, if any.", example = "10") Long tutorId,
        @Schema(description = "Whether a tutor is currently assigned.", example = "true") boolean hasTutor) {
}

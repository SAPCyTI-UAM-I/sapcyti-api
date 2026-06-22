package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Read model for a student program including resolved tutor and advisor names (HU-19).
 */
@Schema(
        name = "StudentProgramResponse",
        description = "Full student program details with resolved tutor and advisor names.")
public record StudentProgramResponse(
        @Schema(description = "Student program identifier.", example = "100") Long id,
        @Schema(description = "Owning student identifier.", example = "50") Long studentId,
        @Schema(description = "Graduate program tenant identifier.", example = "1") Long graduateProgramId,
        @Schema(description = "Enrollment or matricula identifier.", example = "2123803361") String enrollmentId,
        @Schema(description = "Program level. Immutable after creation.", example = "MAESTRIA") ProgramType programType,
        @Schema(description = "Date the student was admitted to the program.", example = "2023-09-01") LocalDate admissionDate,
        @Schema(description = "Expected or actual graduation date.", example = "2026-07-15") LocalDate graduationDate,
        @Schema(description = "Research area or thesis topic.", example = "Inteligencia Artificial") String researchArea,
        @Schema(description = "Program lifecycle status.", example = "ACTIVO") ProgramStatus status,
        @Schema(description = "Withdrawal reason when status is BAJA.", example = "Abandono") String withdrawalReason,
        @Schema(description = "Assigned tutor professor id, if any.", example = "10") Long tutorId,
        @Schema(description = "Resolved tutor display data.") ProfessorReferenceResponse tutor,
        @Schema(description = "Assigned advisor professor ids.") List<Long> advisorIds,
        @Schema(description = "Resolved advisor display data in the same order as advisorIds.")
        List<ProfessorReferenceResponse> advisors) {
}

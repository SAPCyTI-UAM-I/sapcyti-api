package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;

/**
 * Request body for updating a student program (HU-20).
 */
@Schema(
        name = "UpdateStudentProgramRequest",
        description = "Command to update program metadata and replace tutor/advisor assignments atomically.")
public record UpdateStudentProgramRequest(
        @Schema(description = "Date the student was admitted to the program.", example = "2023-09-01")
        @NotNull LocalDate admissionDate,
        @Schema(description = "Expected or actual graduation date. Must be on or after admissionDate.", example = "2026-07-15")
        LocalDate graduationDate,
        @Schema(description = "Research area or thesis topic.", example = "Inteligencia Artificial", maxLength = 200)
        @Size(max = 200) String researchArea,
        @Schema(description = "Program lifecycle status.", example = "ACTIVO")
        @NotNull ProgramStatus status,
        @Schema(
                description = "Reason for withdrawal. Required and non-blank when status is BAJA.",
                example = "Abandono",
                maxLength = 500)
        @Size(max = 500) String withdrawalReason,
        @Schema(
                description = "Professor id assigned as tutor. Send null to clear the current tutor.",
                example = "10")
        Long tutorId,
        @Schema(
                description = "Full replacement list of advisor professor ids. Must be unique. Send an empty list to "
                        + "remove all advisors.",
                example = "[10, 11]")
        @NotNull List<Long> advisorIds) {
}

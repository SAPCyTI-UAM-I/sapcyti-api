package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Read-model projection of a professor for tutor/advisor display (HU-19).
 */
@Schema(name = "ProfessorReference", description = "Resolved professor identity for tutor/advisor display.")
public record ProfessorReferenceResponse(
        @Schema(description = "Professor identifier.", example = "10") Long id,
        @Schema(description = "Professor given name.", example = "Humberto") String firstName,
        @Schema(description = "Professor first surname.", example = "Cervantes") String firstLastName,
        @Schema(description = "Professor second surname.", example = "Maceda") String secondLastName,
        @Schema(description = "Whether the linked professor account is active.", example = "true") boolean active) {
}

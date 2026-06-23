package mx.uam.sapcyti.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard JSON error payload for REST APIs (SPEC-007).
 */
@Schema(name = "ErrorResponse", description = "Standard API error payload.")
public record ErrorResponse(
        @Schema(description = "Machine-readable error code.", example = "NOT_FOUND") String error,
        @Schema(description = "Human-readable error message.", example = "Student program not found") String message) {
}

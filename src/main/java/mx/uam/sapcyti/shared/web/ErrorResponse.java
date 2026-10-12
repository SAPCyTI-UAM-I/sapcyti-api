package mx.uam.sapcyti.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard JSON error payload for REST APIs (SPEC-007).
 *
 * <p>{@code details} is optional (SPEC-041). A null value is omitted so existing
 * endpoints keep publishing only {@code error} and {@code message}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErrorResponse", description = "Standard API error payload.")
public record ErrorResponse(
        @Schema(description = "Machine-readable error code.", example = "NOT_FOUND") String error,
        @Schema(description = "Human-readable error message.", example = "Student program not found") String message,
        @Schema(description = "Optional operation-specific details. Omitted when null.") Object details) {

    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}

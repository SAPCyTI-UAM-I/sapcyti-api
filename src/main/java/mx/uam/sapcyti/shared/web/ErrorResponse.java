package mx.uam.sapcyti.shared.web;

/**
 * Standard JSON error payload for REST APIs (SPEC-007).
 */
public record ErrorResponse(String error, String message) {
}

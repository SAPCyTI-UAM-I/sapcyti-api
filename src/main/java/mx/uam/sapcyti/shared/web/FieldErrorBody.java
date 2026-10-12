package mx.uam.sapcyti.shared.web;

/**
 * One entry of {@code details.fieldErrors} (SPEC-041).
 *
 * @param field   whitelisted field name
 * @param code    whitelisted error code
 * @param message sanitized diagnostic; not a rejected value
 */
public record FieldErrorBody(String field, String code, String message) {
}

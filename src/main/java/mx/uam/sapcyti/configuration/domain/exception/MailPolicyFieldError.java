package mx.uam.sapcyti.configuration.domain.exception;

/**
 * One whitelisted field error for the mail-policy HTTP contract (SPEC-041).
 *
 * @param field   {@code mode}, {@code version}, {@code body}, or {@code query}
 * @param code    stable code from the operation whitelist
 * @param message sanitized diagnostic; the rejected value is not included
 */
public record MailPolicyFieldError(String field, String code, String message) {
}

package mx.uam.sapcyti.exam.application.exception;

/**
 * One {@code details.fieldErrors} item of the HU-65 draft routes
 * (exam-requests.schema.json#/definitions/ExamDraftFieldError).
 * This whitelist is not {@link ExamRequestFieldError}.
 *
 * @param field   closed field name; an unknown name is reported as body, query or manifest
 * @param code    stable code from the operation whitelist
 * @param message sanitized diagnostic; the rejected value is not included
 */
public record ExamDraftFieldError(String field, String code, String message) {
}

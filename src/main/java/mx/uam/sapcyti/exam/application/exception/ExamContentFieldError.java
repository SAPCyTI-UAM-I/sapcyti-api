package mx.uam.sapcyti.exam.application.exception;

/**
 * One {@code details.fieldErrors} item of the HU-66 content route
 * (exam-requests.schema.json#/definitions/ExamContentFieldError).
 * This whitelist is not {@link ExamDraftFieldError} and not {@link ExamRequestFieldError}.
 *
 * @param field   closed field name; an unknown name is reported as body or query
 * @param code    stable code from the operation whitelist
 * @param message sanitized diagnostic; the rejected value is not included
 */
public record ExamContentFieldError(String field, String code, String message) {
}

package mx.uam.sapcyti.exam.application.exception;

/**
 * One {@code details.fieldErrors} item of the HU-62 reads
 * (exam-requests.schema.json#/definitions/ExamRequestFieldError).
 *
 * @param field   {@code body}, {@code query}, or {@code examType}
 * @param code    stable code from the operation whitelist
 * @param message sanitized diagnostic; the rejected value is not included
 */
public record ExamRequestFieldError(String field, String code, String message) {
}

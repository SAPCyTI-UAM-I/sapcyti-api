package mx.uam.sapcyti.exam.domain.model;

/**
 * Codes emitted by document validation
 * (exam-requests.schema.json#/definitions/DocumentValidationErrorCode).
 * {@code STUDENT_DATA_INCOMPLETE} is not a member: a missing given name or paternal surname
 * reuses that code and does not widen this set.
 */
public enum DocumentValidationErrorCode {
    INVALID_DOCUMENT_NAME,
    UNSAFE_FILE_NAME,
    DOCUMENT_KIND_NOT_ALLOWED,
    INVALID_ARTICLE_URL,
    INVALID_PDF_CONTENT
}

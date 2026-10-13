package mx.uam.sapcyti.exam.domain.model;

import java.util.Objects;

/**
 * Rejection of one document
 * (exam-requests.schema.json#/definitions/DocumentValidationError).
 * {@code details} is present only for {@code INVALID_DOCUMENT_NAME} and {@code INVALID_ARTICLE_URL}.
 */
public record DocumentValidationError(String error, String message, DocumentValidationDetails details) {

    public static final String STUDENT_DATA_INCOMPLETE = "STUDENT_DATA_INCOMPLETE";

    public DocumentValidationError {
        Objects.requireNonNull(error, "error");
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message is blank");
        }
        boolean name = DocumentValidationErrorCode.INVALID_DOCUMENT_NAME.name().equals(error);
        boolean url = DocumentValidationErrorCode.INVALID_ARTICLE_URL.name().equals(error);
        if (name) {
            if (details == null || details.expectedName() == null) {
                throw new IllegalArgumentException("expectedName is required");
            }
        } else if (url) {
            if (details == null || details.fieldErrors() == null) {
                throw new IllegalArgumentException("fieldErrors is required");
            }
        } else if (details != null) {
            throw new IllegalArgumentException("details is omitted for " + error);
        }
    }
}

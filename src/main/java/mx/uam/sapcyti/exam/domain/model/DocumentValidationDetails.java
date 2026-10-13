package mx.uam.sapcyti.exam.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * {@code details} of one document rejection. {@code expectedName} is set only for
 * {@code INVALID_DOCUMENT_NAME}. {@code fieldErrors} is set only for {@code INVALID_ARTICLE_URL}.
 */
public record DocumentValidationDetails(String expectedName, List<DocumentUrlFieldError> fieldErrors) {

    public DocumentValidationDetails {
        if (expectedName != null && fieldErrors != null) {
            throw new IllegalArgumentException("details carries expectedName or fieldErrors");
        }
        if (expectedName == null && (fieldErrors == null || fieldErrors.isEmpty())) {
            throw new IllegalArgumentException("details is empty");
        }
        if (expectedName != null && expectedName.isBlank()) {
            throw new IllegalArgumentException("expectedName is blank");
        }
        fieldErrors = fieldErrors == null ? null : List.copyOf(fieldErrors);
    }

    public static DocumentValidationDetails expectedName(String expectedName) {
        return new DocumentValidationDetails(Objects.requireNonNull(expectedName, "expectedName"), null);
    }

    public static DocumentValidationDetails fieldErrors(List<DocumentUrlFieldError> fieldErrors) {
        return new DocumentValidationDetails(null, fieldErrors);
    }
}

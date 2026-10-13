package mx.uam.sapcyti.exam.domain.model;

import java.util.Objects;

/**
 * One {@code details.fieldErrors} item published only with {@code INVALID_ARTICLE_URL}
 * (exam-requests.schema.json#/definitions/DocumentUrlFieldError).
 * The rejected value is not copied into {@code message}.
 */
public record DocumentUrlFieldError(String field, String code, String message) {

    public static final String FIELD = "externalUrl";
    public static final String REQUIRED = "REQUIRED";
    public static final String INVALID_TYPE = "INVALID_TYPE";
    public static final String INVALID_VALUE = "INVALID_VALUE";
    public static final String HTTPS_REQUIRED = "HTTPS_REQUIRED";

    public DocumentUrlFieldError {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        if (!FIELD.equals(field)) {
            throw new IllegalArgumentException("field must be externalUrl");
        }
    }

    public static DocumentUrlFieldError externalUrl(String code, String message) {
        return new DocumentUrlFieldError(FIELD, code, message);
    }
}

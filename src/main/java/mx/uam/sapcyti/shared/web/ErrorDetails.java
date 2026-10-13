package mx.uam.sapcyti.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Optional {@code details} object for {@link ErrorResponse} (SPEC-041, SPEC-052).
 *
 * <p>Null properties are omitted. {@code fieldErrors} is used by validation.
 * {@code currentVersion} is used by a mail-policy version conflict.
 * {@code expectedName} is used by an exam-draft {@code INVALID_DOCUMENT_NAME}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetails(List<FieldErrorBody> fieldErrors, Integer currentVersion, String expectedName) {

    public ErrorDetails(List<FieldErrorBody> fieldErrors, Integer currentVersion) {
        this(fieldErrors, currentVersion, null);
    }
}

package mx.uam.sapcyti.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Optional {@code details} object for {@link ErrorResponse} (SPEC-041).
 *
 * <p>Null properties are omitted. {@code fieldErrors} is used by validation;
 * {@code currentVersion} is used by a mail-policy version conflict.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetails(List<FieldErrorBody> fieldErrors, Integer currentVersion) {
}

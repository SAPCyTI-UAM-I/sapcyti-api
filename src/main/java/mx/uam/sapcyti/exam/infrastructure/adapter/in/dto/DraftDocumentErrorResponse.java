package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import mx.uam.sapcyti.shared.web.FieldErrorBody;

/**
 * One {@code results[].error}. {@code details} is omitted when the code has none.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DraftDocumentErrorResponse(String error, String message, Details details) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Details(String expectedName, List<FieldErrorBody> fieldErrors) {
    }
}

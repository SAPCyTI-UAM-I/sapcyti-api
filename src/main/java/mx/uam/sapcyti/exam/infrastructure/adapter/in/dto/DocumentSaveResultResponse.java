package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import mx.uam.sapcyti.exam.domain.model.DocumentType;

/**
 * One entry of {@code SaveDocumentsResponse.results}, in manifest order.
 * {@code SAVED} carries {@code document}. {@code ERROR} carries {@code error}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentSaveResultResponse(
        DocumentType documentType,
        String status,
        CurrentExamDocumentResponse document,
        DraftDocumentErrorResponse error) {
}

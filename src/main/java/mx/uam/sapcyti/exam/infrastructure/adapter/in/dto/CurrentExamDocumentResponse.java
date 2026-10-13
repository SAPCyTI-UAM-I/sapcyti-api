package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import java.time.Instant;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;

/**
 * Current document metadata. No storage key, bucket or filesystem path.
 */
public record CurrentExamDocumentResponse(
        Long id,
        DocumentType documentType,
        DocumentKind kind,
        String originalName,
        String externalUrl,
        Long sizeBytes,
        Instant uploadedAt) {

    public static CurrentExamDocumentResponse from(CurrentExamDocument document) {
        return new CurrentExamDocumentResponse(
                document.id(),
                document.documentType(),
                document.kind(),
                document.originalName(),
                document.externalUrl(),
                document.sizeBytes(),
                document.uploadedAt());
    }
}

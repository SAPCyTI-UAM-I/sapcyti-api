package mx.uam.sapcyti.exam.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Current document metadata for one type (exam-requests.schema.json#/definitions/CurrentExamDocument).
 * No storage key, bucket or filesystem path.
 */
public record CurrentExamDocument(
        Long id,
        DocumentType documentType,
        DocumentKind kind,
        String originalName,
        String externalUrl,
        Long sizeBytes,
        Instant uploadedAt) {

    public CurrentExamDocument {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
        if (kind == DocumentKind.PDF) {
            if (originalName == null || originalName.isBlank() || externalUrl != null
                    || sizeBytes == null || sizeBytes < 0) {
                throw new IllegalArgumentException("PDF requires a name and size, and no URL");
            }
        } else if (originalName != null || sizeBytes != null
                || externalUrl == null || !externalUrl.startsWith("https://")) {
            throw new IllegalArgumentException("URL requires an https URL and no file name or size");
        }
    }
}

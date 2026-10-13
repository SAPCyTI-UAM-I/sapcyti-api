package mx.uam.sapcyti.exam.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Current document row for one request and type
 * (exam-requests.schema.json#/definitions/StoredDocumentReference).
 * This is not a response DTO. {@link CurrentExamDocument} is the public projection.
 */
public record StoredDocumentReference(
        Long id,
        Long examRequestId,
        DocumentType documentType,
        DocumentKind kind,
        String objectKey,
        String sha256,
        String contentType,
        String originalName,
        String externalUrl,
        Long sizeBytes,
        Instant uploadedAt) {

    public static final String PDF_MEDIA_TYPE = "application/pdf";

    public StoredDocumentReference {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (examRequestId == null || examRequestId < 1) {
            throw new IllegalArgumentException("examRequestId must be positive");
        }
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
        if (kind == DocumentKind.PDF) {
            if (objectKey == null || objectKey.isBlank()) {
                throw new IllegalArgumentException("PDF requires an object key");
            }
            sha256 = Sha256Hashes.require(sha256);
            if (!PDF_MEDIA_TYPE.equals(contentType)) {
                throw new IllegalArgumentException("PDF content type is application/pdf");
            }
            if (originalName == null || originalName.isBlank()) {
                throw new IllegalArgumentException("PDF requires the received name");
            }
            if (externalUrl != null) {
                throw new IllegalArgumentException("PDF has no external URL");
            }
            if (sizeBytes == null || sizeBytes < 1) {
                throw new IllegalArgumentException("PDF size must be at least 1");
            }
        } else if (objectKey != null || sha256 != null || contentType != null
                || originalName != null || sizeBytes != null
                || externalUrl == null || !externalUrl.startsWith("https://")) {
            throw new IllegalArgumentException("URL stores the https URL and no object");
        }
    }
}

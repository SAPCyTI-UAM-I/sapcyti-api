package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.StoredDocumentReference;

/**
 * Current document of one type. The id is reserved before the object put.
 * A replaced id is deleted here; it is not a second visible row.
 */
@Entity
@Table(name = "exam_request_documents", uniqueConstraints = {
        @UniqueConstraint(name = "uq_exam_request_documents_type", columnNames = {"exam_request_id", "document_type"})
})
class ExamDocumentEntity {

    @Id
    private Long id;

    @Column(name = "exam_request_id", nullable = false)
    private Long examRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 32)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 8)
    private DocumentKind kind;

    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "external_url", length = 2048)
    private String externalUrl;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected ExamDocumentEntity() {
        // JPA
    }

    static ExamDocumentEntity from(StoredDocumentReference reference) {
        ExamDocumentEntity entity = new ExamDocumentEntity();
        entity.id = reference.id();
        entity.examRequestId = reference.examRequestId();
        entity.documentType = reference.documentType();
        entity.kind = reference.kind();
        entity.objectKey = reference.objectKey();
        entity.sha256 = reference.sha256();
        entity.contentType = reference.contentType();
        entity.originalName = reference.originalName();
        entity.externalUrl = reference.externalUrl();
        entity.sizeBytes = reference.sizeBytes();
        entity.uploadedAt = reference.uploadedAt();
        return entity;
    }

    Long getId() {
        return id;
    }

    DocumentType getDocumentType() {
        return documentType;
    }

    DocumentKind getKind() {
        return kind;
    }

    String getObjectKey() {
        return objectKey;
    }

    String getSha256() {
        return sha256;
    }

    String getContentType() {
        return contentType;
    }

    String getOriginalName() {
        return originalName;
    }

    String getExternalUrl() {
        return externalUrl;
    }

    Long getSizeBytes() {
        return sizeBytes;
    }

    Instant getUploadedAt() {
        return uploadedAt;
    }
}

package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * PDF key that a committed replace or delete stopped using.
 * The row is written in that same database transaction and is not part of any read.
 */
@Entity
@Table(name = "exam_request_superseded_objects")
class SupersededObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "superseded_at", nullable = false)
    private Instant supersededAt;

    protected SupersededObjectEntity() {
        // JPA
    }

    static SupersededObjectEntity of(String objectKey, String sha256, Instant supersededAt) {
        SupersededObjectEntity entity = new SupersededObjectEntity();
        entity.objectKey = objectKey;
        entity.sha256 = sha256;
        entity.supersededAt = supersededAt;
        return entity;
    }

    String getObjectKey() {
        return objectKey;
    }

    String getSha256() {
        return sha256;
    }

    Instant getSupersededAt() {
        return supersededAt;
    }
}

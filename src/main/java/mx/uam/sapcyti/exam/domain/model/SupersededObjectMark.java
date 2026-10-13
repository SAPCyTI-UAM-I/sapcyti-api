package mx.uam.sapcyti.exam.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Internal mark written in the same database transaction that replaces or removes a PDF
 * (exam-requests.schema.json#/definitions/SupersededObjectMark).
 * It is omitted from every read. A URL has no object and writes no mark.
 */
public record SupersededObjectMark(String objectKey, String sha256, Instant supersededAt) {

    public SupersededObjectMark {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        sha256 = Sha256Hashes.require(sha256);
        Objects.requireNonNull(supersededAt, "supersededAt");
    }
}

package mx.uam.sapcyti.exam.domain.model;

/**
 * Result of a complete private object write
 * (exam-requests.schema.json#/definitions/StoredObjectReceipt).
 * A mismatch or an incomplete write is not a receipt.
 */
public record StoredObjectReceipt(String objectKey, String sha256, String contentType, long sizeBytes) {

    public StoredObjectReceipt {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        sha256 = Sha256Hashes.require(sha256);
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType is required");
        }
        if (sizeBytes < 1) {
            throw new IllegalArgumentException("sizeBytes must be at least 1");
        }
    }
}

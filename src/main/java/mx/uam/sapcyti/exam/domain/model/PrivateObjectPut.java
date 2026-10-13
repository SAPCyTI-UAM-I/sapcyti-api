package mx.uam.sapcyti.exam.domain.model;

/**
 * Command of {@code DocumentStoragePort}
 * (exam-requests.schema.json#/definitions/PrivateObjectPut).
 *
 * <p>There is no maximum {@code sizeBytes}. The 20 MB figure is a mail-provider limit.
 */
public record PrivateObjectPut(String objectKey, String sha256, String contentType, long sizeBytes) {

    public PrivateObjectPut {
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

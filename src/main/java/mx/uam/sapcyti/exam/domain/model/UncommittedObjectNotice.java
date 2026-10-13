package mx.uam.sapcyti.exam.domain.model;

/**
 * Names an object whose put succeeded and whose database commit did not
 * (exam-requests.schema.json#/definitions/UncommittedObjectNotice).
 * It does not insert {@link SupersededObjectMark} and it is not a response DTO.
 */
public record UncommittedObjectNotice(String objectKey, String sha256) {

    public UncommittedObjectNotice {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        sha256 = Sha256Hashes.require(sha256);
    }
}

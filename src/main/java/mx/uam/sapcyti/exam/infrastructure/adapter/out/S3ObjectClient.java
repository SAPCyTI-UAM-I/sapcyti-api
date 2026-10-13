package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import java.io.InputStream;

/**
 * Writes one new private object. The production client is not declared in this cut:
 * tests inject a double. This type has no endpoint, bucket or credentials and does not create a bucket.
 */
public interface S3ObjectClient {

    /**
     * Reads {@code body} to completion. Fails when {@code objectKey} is already stored.
     * Does not overwrite that key.
     */
    void putNew(String objectKey, String contentType, InputStream body, long sizeBytes);
}

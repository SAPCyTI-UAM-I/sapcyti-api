package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import java.io.InputStream;
import mx.uam.sapcyti.exam.domain.model.OpenedObject;

/**
 * Writes and opens one private object. The production client is not declared in this cut:
 * tests inject a double. This type has no endpoint, bucket or credentials and does not create a bucket.
 */
public interface S3ObjectClient {

    /**
     * Reads {@code body} to completion. Fails when {@code objectKey} is already stored.
     * Does not overwrite that key.
     */
    void putNew(String objectKey, String contentType, InputStream body, long sizeBytes);

    /**
     * Opens the object stored at {@code objectKey}. The size is the full object.
     * A missing object fails and does not return a stream.
     */
    OpenedObject open(String objectKey);
}

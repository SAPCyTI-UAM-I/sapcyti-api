package mx.uam.sapcyti.exam.domain.exception;

/**
 * The private object was not stored completely. There is no {@code StoredObjectReceipt}.
 * The message does not carry the object key.
 */
public class ObjectStorageException extends RuntimeException {

    public ObjectStorageException(String message) {
        super(message);
    }

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

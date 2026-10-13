package mx.uam.sapcyti.exam.domain.model;

import java.io.InputStream;
import java.util.Objects;

/**
 * Private object opened by {@code DocumentStoragePort.open} before any response header is fixed.
 * {@code sizeBytes} is the object size, not a partial range.
 */
public record OpenedObject(InputStream body, long sizeBytes) {

    public OpenedObject {
        Objects.requireNonNull(body, "body");
        if (sizeBytes < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
    }
}

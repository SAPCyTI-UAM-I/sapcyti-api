package mx.uam.sapcyti.exam.domain.port.out;

import java.io.InputStream;
import mx.uam.sapcyti.exam.domain.model.PrivateObjectPut;
import mx.uam.sapcyti.exam.domain.model.StoredObjectReceipt;
import mx.uam.sapcyti.exam.domain.model.UncommittedObjectNotice;

/**
 * Writes one new immutable private object, then names a key whose database commit did not follow.
 * The database transaction is not this port. The adapter does not create the bucket.
 */
public interface DocumentStoragePort {

    /**
     * Writes a new object and returns a receipt only after the write is complete.
     * Does not overwrite an existing key. There is no size ceiling.
     * An incomplete write or a mismatch throws and is not a receipt.
     */
    StoredObjectReceipt put(PrivateObjectPut command, InputStream body);

    /**
     * Names the key whose put already happened and whose commit did not.
     * Does not delete the object and does not insert a superseded mark.
     */
    UncommittedObjectNotice noticeUncommitted(String objectKey, String sha256);
}

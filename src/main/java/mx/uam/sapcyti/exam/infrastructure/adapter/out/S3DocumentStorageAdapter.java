package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import mx.uam.sapcyti.exam.domain.exception.ObjectStorageException;
import mx.uam.sapcyti.exam.domain.model.PrivateObjectPut;
import mx.uam.sapcyti.exam.domain.model.StoredObjectReceipt;
import mx.uam.sapcyti.exam.domain.model.UncommittedObjectNotice;
import mx.uam.sapcyti.exam.domain.port.out.DocumentStoragePort;

/**
 * {@link DocumentStoragePort} for a private S3-compatible bucket.
 * Not a Spring bean: this cut does not declare the client, the endpoint, the bucket or credentials,
 * and it does not create the bucket. There is no maximum object size.
 */
public final class S3DocumentStorageAdapter implements DocumentStoragePort {

    private final S3ObjectClient client;

    public S3DocumentStorageAdapter(S3ObjectClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public StoredObjectReceipt put(PrivateObjectPut command, InputStream body) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(body, "body");
        Path spool = null;
        try {
            spool = Files.createTempFile("exam-object-", ".bin");
            DigestedCopy copy = spool(body, spool);
            if (copy.count() != command.sizeBytes() || !copy.sha256().equals(command.sha256())) {
                throw new ObjectStorageException("Object write did not match");
            }
            try (InputStream stored = Files.newInputStream(spool)) {
                client.putNew(command.objectKey(), command.contentType(), stored, command.sizeBytes());
            }
            return new StoredObjectReceipt(
                    command.objectKey(), command.sha256(), command.contentType(), command.sizeBytes());
        } catch (ObjectStorageException ex) {
            throw ex;
        } catch (RuntimeException | IOException ex) {
            throw new ObjectStorageException("Object write failed", ex);
        } finally {
            deleteSpool(spool);
        }
    }

    @Override
    public UncommittedObjectNotice noticeUncommitted(String objectKey, String sha256) {
        return new UncommittedObjectNotice(objectKey, sha256);
    }

    private static DigestedCopy spool(InputStream body, Path spool) throws IOException {
        MessageDigest digest = sha256();
        long count = 0;
        try (InputStream in = body; OutputStream out = Files.newOutputStream(spool)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
                count += read;
            }
        }
        return new DigestedCopy(count, hex(digest.digest()));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }

    private static String hex(byte[] raw) {
        StringBuilder out = new StringBuilder(raw.length * 2);
        for (byte value : raw) {
            out.append(Character.forDigit((value >> 4) & 0xf, 16));
            out.append(Character.forDigit(value & 0xf, 16));
        }
        return out.toString();
    }

    private static void deleteSpool(Path spool) {
        if (spool == null) {
            return;
        }
        try {
            Files.deleteIfExists(spool);
        } catch (IOException ex) {
            // A leftover spool is not a bucket object. The key is not logged.
        }
    }

    private record DigestedCopy(long count, String sha256) {
    }
}

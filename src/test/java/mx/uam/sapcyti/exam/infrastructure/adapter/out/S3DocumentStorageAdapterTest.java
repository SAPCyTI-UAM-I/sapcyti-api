package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import mx.uam.sapcyti.exam.domain.exception.ObjectStorageException;
import mx.uam.sapcyti.exam.domain.model.PrivateObjectPut;
import mx.uam.sapcyti.exam.domain.model.StoredDocumentReference;
import mx.uam.sapcyti.exam.domain.model.StoredObjectReceipt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

/**
 * SPEC-051: the S3 adapter returns a receipt for an object larger than 20 MB.
 * The client is a double. This cut does not declare a bucket.
 */
class S3DocumentStorageAdapterTest {

    private static final long OVER_TWENTY_MB = 20L * 1024 * 1024 + 1;
    private static final String KEY = "17/10/exam-requests/1/2.pdf";

    private final FakeS3 client = new FakeS3();
    private final S3DocumentStorageAdapter adapter = new S3DocumentStorageAdapter(client);

    @Test
    @DisplayName("a PDF larger than 20 MB is stored")
    void pdfLargerThanTwentyMbIsStored() {
        String sha = sha256OfZeros(OVER_TWENTY_MB);
        PrivateObjectPut command = new PrivateObjectPut(KEY, sha, StoredDocumentReference.PDF_MEDIA_TYPE, OVER_TWENTY_MB);

        StoredObjectReceipt receipt = adapter.put(command, zeros(OVER_TWENTY_MB));

        assertThat(receipt.sizeBytes()).isGreaterThan(20L * 1024 * 1024);
        assertThat(receipt.objectKey()).isEqualTo(KEY);
        assertThat(receipt.sha256()).isEqualTo(sha);
        assertThat(receipt.contentType()).isEqualTo(StoredDocumentReference.PDF_MEDIA_TYPE);
        assertThat(client.objects).containsEntry(KEY, OVER_TWENTY_MB);
    }

    @Test
    @DisplayName("an incomplete write is not a receipt and does not call the client")
    void incompleteWriteIsNotAReceipt() {
        PrivateObjectPut command = new PrivateObjectPut(
                KEY, sha256OfZeros(4), StoredDocumentReference.PDF_MEDIA_TYPE, 4);

        assertThatThrownBy(() -> adapter.put(command, zeros(2)))
                .isInstanceOf(ObjectStorageException.class);
        assertThat(client.objects).isEmpty();
    }

    @Test
    @DisplayName("an existing key is not overwritten")
    void existingKeyIsNotOverwritten() {
        PrivateObjectPut command = new PrivateObjectPut(
                KEY, sha256OfZeros(4), StoredDocumentReference.PDF_MEDIA_TYPE, 4);
        adapter.put(command, zeros(4));

        assertThatThrownBy(() -> adapter.put(command, zeros(4)))
                .isInstanceOf(ObjectStorageException.class);
        assertThat(client.objects).containsEntry(KEY, 4L);
    }

    @Test
    @DisplayName("noticeUncommitted names the new key and does not delete the object")
    void noticeNamesTheKeyAndDoesNotDelete() {
        PrivateObjectPut command = new PrivateObjectPut(
                KEY, sha256OfZeros(4), StoredDocumentReference.PDF_MEDIA_TYPE, 4);
        adapter.put(command, zeros(4));

        assertThat(adapter.noticeUncommitted(KEY, command.sha256()).objectKey()).isEqualTo(KEY);
        assertThat(client.objects).containsEntry(KEY, 4L);
        assertThat(S3DocumentStorageAdapter.class.getDeclaredFields()).extracting("name").containsExactly("client");
        assertThat(S3DocumentStorageAdapter.class.getAnnotation(Component.class)).isNull();
        assertThat(S3DocumentStorageAdapter.class.getAnnotation(Repository.class)).isNull();
    }

    private static InputStream zeros(long size) {
        return new InputStream() {
            private long remaining = size;

            @Override
            public int read() {
                if (remaining == 0) {
                    return -1;
                }
                remaining--;
                return 0;
            }

            @Override
            public int read(byte[] buffer, int off, int len) {
                if (remaining == 0) {
                    return -1;
                }
                int count = (int) Math.min(len, remaining);
                Arrays.fill(buffer, off, off + count, (byte) 0);
                remaining -= count;
                return count;
            }
        };
    }

    private static String sha256OfZeros(long size) {
        MessageDigest digest = sha256();
        byte[] chunk = new byte[8192];
        long remaining = size;
        while (remaining > 0) {
            int count = (int) Math.min(chunk.length, remaining);
            digest.update(chunk, 0, count);
            remaining -= count;
        }
        return hex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
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

    private static final class FakeS3 implements S3ObjectClient {

        private final Map<String, Long> objects = new LinkedHashMap<>();

        @Override
        public void putNew(String objectKey, String contentType, InputStream body, long sizeBytes) {
            if (objects.containsKey(objectKey)) {
                throw new IllegalStateException("object already stored");
            }
            try {
                long read = body.transferTo(OutputStream.nullOutputStream());
                if (read != sizeBytes) {
                    throw new IllegalStateException("incomplete object");
                }
            } catch (IOException ex) {
                throw new IllegalStateException("object write failed", ex);
            }
            objects.put(objectKey, sizeBytes);
        }
    }
}

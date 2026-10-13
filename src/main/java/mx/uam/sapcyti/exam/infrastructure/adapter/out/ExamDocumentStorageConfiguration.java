package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import java.io.InputStream;
import java.util.function.Supplier;
import mx.uam.sapcyti.exam.domain.exception.ObjectStorageException;
import mx.uam.sapcyti.exam.domain.model.OpenedObject;
import mx.uam.sapcyti.exam.domain.port.out.DocumentStoragePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the SPEC-051 storage adapter. This cut does not declare an endpoint, a bucket or
 * credentials and does not create the bucket. Tests inject an {@link S3ObjectClient} double.
 * Without that client, a put fails and no receipt is returned.
 */
@Configuration
public class ExamDocumentStorageConfiguration {

    @Bean
    public DocumentStoragePort documentStoragePort(ObjectProvider<S3ObjectClient> clients) {
        S3ObjectClient client = clients.getIfAvailable(unconfigured());
        return new S3DocumentStorageAdapter(client);
    }

    private static Supplier<S3ObjectClient> unconfigured() {
        return () -> new S3ObjectClient() {
            @Override
            public void putNew(String objectKey, String contentType, InputStream body, long sizeBytes) {
                reject(body);
            }

            @Override
            public OpenedObject open(String objectKey) {
                throw new ObjectStorageException("Object storage is not configured");
            }
        };
    }

    private static void reject(InputStream body) {
        throw new ObjectStorageException("Object storage is not configured");
    }
}

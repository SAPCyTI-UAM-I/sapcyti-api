package mx.uam.sapcyti.exam.application.model;

import java.util.List;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;

/**
 * Envelope already accepted for a HU-65 write. An invalid envelope never becomes one of these.
 */
public final class DraftCommands {

    private DraftCommands() {
    }

    /**
     * One POST batch. {@code version} is 0 only when no draft exists.
     */
    public record Batch(int version, List<Document> documents) {

        public Batch {
            documents = List.copyOf(documents);
        }
    }

    /**
     * One descriptor whose binary part, when {@code kind} is PDF, was already read.
     * A URL has no bytes and no file name.
     */
    public record Document(
            DocumentType documentType,
            DocumentKind kind,
            String originalName,
            byte[] pdfBytes,
            String externalUrl) {
    }

    /**
     * PUT of one PDF. {@code version} is at least 1.
     */
    public record PdfReplacement(int version, String originalName, byte[] pdfBytes) {
    }

    /**
     * PUT of one article URL. {@code version} is at least 1.
     */
    public record UrlReplacement(int version, String externalUrl) {
    }
}

package mx.uam.sapcyti.exam.domain.model;

import java.util.Objects;

/**
 * Outcome of validating one document
 * (exam-requests.schema.json#/definitions/DocumentValidationVerdict).
 * An accepted verdict has no error and does not rename the file.
 * The verdict does not write an object, create a draft, assign a folio or send mail.
 */
public record DocumentValidationVerdict(
        DocumentType documentType, boolean accepted, DocumentValidationError error) {

    public DocumentValidationVerdict {
        Objects.requireNonNull(documentType, "documentType");
        if (accepted && error != null) {
            throw new IllegalArgumentException("accepted verdict has no error");
        }
        if (!accepted && error == null) {
            throw new IllegalArgumentException("rejected verdict requires an error");
        }
    }

    public static DocumentValidationVerdict accepted(DocumentType documentType) {
        return new DocumentValidationVerdict(documentType, true, null);
    }

    public static DocumentValidationVerdict rejected(DocumentType documentType, DocumentValidationError error) {
        return new DocumentValidationVerdict(documentType, false, error);
    }
}

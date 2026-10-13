package mx.uam.sapcyti.exam.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Missing required documents for a later submit of one binding
 * (exam-requests.schema.json#/definitions/SubmitCompleteness).
 * An empty list means the binding is complete. This value does not change status or folio.
 */
public record SubmitCompleteness(
        ExamType examType,
        Degree degree,
        List<DocumentType> missingDocumentTypes) {

    public SubmitCompleteness {
        Objects.requireNonNull(examType, "examType");
        Objects.requireNonNull(degree, "degree");
        Objects.requireNonNull(missingDocumentTypes, "missingDocumentTypes");
        if (new HashSet<>(missingDocumentTypes).size() != missingDocumentTypes.size()) {
            throw new IllegalArgumentException("missingDocumentTypes must be unique");
        }
        missingDocumentTypes = List.copyOf(missingDocumentTypes);
    }
}

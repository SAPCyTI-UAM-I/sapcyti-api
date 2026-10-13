package mx.uam.sapcyti.exam.domain.model;

import java.util.Objects;

/**
 * Whether one type and one kind are allowed for the resolved degree and exam type
 * (exam-requests.schema.json#/definitions/DocumentKindAdmission).
 * {@code required} does not change this decision.
 */
public record DocumentKindAdmission(
        ExamType examType,
        Degree degree,
        DocumentType documentType,
        DocumentKind kind,
        boolean admitted) {

    public DocumentKindAdmission {
        Objects.requireNonNull(examType, "examType");
        Objects.requireNonNull(degree, "degree");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(kind, "kind");
    }
}

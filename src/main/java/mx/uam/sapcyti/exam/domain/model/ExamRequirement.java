package mx.uam.sapcyti.exam.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * One catalog row joined with the student's current document, if any
 * (exam-requests.schema.json#/definitions/ExamRequirement).
 */
public record ExamRequirement(
        DocumentType documentType,
        boolean required,
        List<DocumentKind> acceptedKinds,
        String expectedName,
        String exampleName,
        RequirementStatus status,
        CurrentExamDocument currentDocument) {

    public ExamRequirement {
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(acceptedKinds, "acceptedKinds");
        Objects.requireNonNull(status, "status");
        if (acceptedKinds.isEmpty() || new HashSet<>(acceptedKinds).size() != acceptedKinds.size()) {
            throw new IllegalArgumentException("acceptedKinds must be unique and non-empty");
        }
        acceptedKinds = List.copyOf(acceptedKinds);
        if (documentType == DocumentType.ARTICULO) {
            if (expectedName != null || exampleName != null) {
                throw new IllegalArgumentException("ARTICULO has no prescribed name");
            }
        } else if (expectedName == null || expectedName.isBlank() || !expectedName.equals(exampleName)) {
            throw new IllegalArgumentException("expectedName and exampleName must be the same file name");
        }
        if (status == RequirementStatus.PENDIENTE && currentDocument != null) {
            throw new IllegalArgumentException("PENDIENTE has no current document");
        }
        if (status == RequirementStatus.CARGADO && currentDocument == null) {
            throw new IllegalArgumentException("CARGADO requires a current document");
        }
        if (currentDocument != null && currentDocument.documentType() != documentType) {
            throw new IllegalArgumentException("current document type must match");
        }
    }
}

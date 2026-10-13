package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import java.util.List;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamRequirement;
import mx.uam.sapcyti.exam.domain.model.RequirementStatus;

/**
 * One catalog row joined with the student's current document, if any.
 * {@code currentDocument} is null when the status is {@code PENDIENTE}.
 */
public record ExamRequirementResponse(
        DocumentType documentType,
        boolean required,
        List<DocumentKind> acceptedKinds,
        String expectedName,
        String exampleName,
        RequirementStatus status,
        CurrentExamDocumentResponse currentDocument) {

    public static ExamRequirementResponse from(ExamRequirement requirement) {
        CurrentExamDocumentResponse document = requirement.currentDocument() == null
                ? null
                : CurrentExamDocumentResponse.from(requirement.currentDocument());
        return new ExamRequirementResponse(
                requirement.documentType(),
                requirement.required(),
                requirement.acceptedKinds(),
                requirement.expectedName(),
                requirement.exampleName(),
                requirement.status(),
                document);
    }
}

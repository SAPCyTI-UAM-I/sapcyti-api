package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

/**
 * Body of a successful PUT or DELETE. {@code document} is null after DELETE.
 */
public record DraftMutationResponse(
        ExamRequestViewResponse request, int version, CurrentExamDocumentResponse document) {
}

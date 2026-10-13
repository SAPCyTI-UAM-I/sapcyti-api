package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import java.util.List;
import mx.uam.sapcyti.exam.application.model.DraftOutcomes;
import mx.uam.sapcyti.exam.domain.model.ExamRequestSummary;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;
import mx.uam.sapcyti.shared.web.FieldErrorBody;

/**
 * Maps draft use-case results to the HU-65 response records.
 */
public final class ExamDraftResponses {

    private ExamDraftResponses() {
    }

    public static SaveDocumentsResponse save(DraftOutcomes.Save result) {
        return new SaveDocumentsResponse(view(result.state()), result.version(), result.results().stream()
                .map(ExamDraftResponses::item)
                .toList());
    }

    public static DraftMutationResponse mutation(DraftOutcomes.Mutation result) {
        ExamRequestViewResponse request = view(result.state());
        CurrentExamDocumentResponse document = result.document() == null
                ? null
                : CurrentExamDocumentResponse.from(result.document());
        return new DraftMutationResponse(request, request.version(), document);
    }

    public static ExamPanelResponse panel(DraftOutcomes.Panel result) {
        ExamRequirements state = result.state();
        List<ExamRequirementResponse> requirements = requirements(state);
        return new ExamPanelResponse(
                state.examType(),
                state.degree(),
                state.programStatus(),
                state.eligible(),
                state.eligibilityReason(),
                view(state),
                requirements,
                result.requiredPendingCount());
    }

    private static DocumentSaveResultResponse item(DraftOutcomes.Item item) {
        if (item.error() != null) {
            return new DocumentSaveResultResponse(
                    item.documentType(), "ERROR", null, error(item.error()));
        }
        return new DocumentSaveResultResponse(
                item.documentType(),
                "SAVED",
                CurrentExamDocumentResponse.from(item.document()),
                null);
    }

    private static DraftDocumentErrorResponse error(DraftOutcomes.ItemError error) {
        DraftDocumentErrorResponse.Details details = null;
        if (error.expectedName() != null) {
            details = new DraftDocumentErrorResponse.Details(error.expectedName(), null);
        } else if (error.fieldErrors() != null) {
            List<FieldErrorBody> fieldErrors = error.fieldErrors().stream()
                    .map(field -> new FieldErrorBody(field.field(), field.code(), field.message()))
                    .toList();
            details = new DraftDocumentErrorResponse.Details(null, fieldErrors);
        }
        return new DraftDocumentErrorResponse(error.code(), error.message(), details);
    }

    private static ExamRequestViewResponse view(ExamRequirements state) {
        if (state == null || state.request() == null) {
            return null;
        }
        ExamRequestSummary request = state.request();
        return new ExamRequestViewResponse(
                request.id(),
                request.examType(),
                request.degree(),
                request.status(),
                request.folio(),
                request.version(),
                request.submittedAt(),
                requirements(state),
                null);
    }

    private static List<ExamRequirementResponse> requirements(ExamRequirements state) {
        return state.requirements().stream().map(ExamRequirementResponse::from).toList();
    }
}

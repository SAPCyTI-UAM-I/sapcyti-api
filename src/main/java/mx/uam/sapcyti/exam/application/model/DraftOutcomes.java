package mx.uam.sapcyti.exam.application.model;

import java.util.List;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.DocumentUrlFieldError;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;

/**
 * Results of the HU-65 draft use case. Storage keys are not part of these records.
 */
public final class DraftOutcomes {

    private DraftOutcomes() {
    }

    /**
     * Accepted POST. {@code state} is null when the batch left no draft.
     * {@code version} is 0 in that case and otherwise equals the stored version.
     */
    public record Save(ExamRequirements state, int version, List<Item> results) {

        public Save {
            results = List.copyOf(results);
        }
    }

    /**
     * One manifest entry. {@code document} is set only for {@code SAVED}.
     * {@code error} is set only for {@code ERROR}.
     */
    public record Item(DocumentType documentType, CurrentExamDocument document, ItemError error) {
    }

    /**
     * One {@code results[].error}. {@code expectedName} and {@code fieldErrors} are not both set.
     */
    public record ItemError(
            String code,
            String message,
            String expectedName,
            List<DocumentUrlFieldError> fieldErrors) {
    }

    /**
     * GET panel. {@code state.request} is null when no draft exists. The call did not insert a row.
     */
    public record Panel(ExamRequirements state, int requiredPendingCount) {
    }

    /**
     * PUT or DELETE. {@code document} is null after DELETE.
     */
    public record Mutation(ExamRequirements state, CurrentExamDocument document) {
    }
}

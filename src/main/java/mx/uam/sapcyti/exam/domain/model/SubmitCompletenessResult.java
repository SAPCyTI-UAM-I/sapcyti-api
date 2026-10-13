package mx.uam.sapcyti.exam.domain.model;

/**
 * Outcome of {@code ExamSubmitCompleteness}. {@code PROGRAM_NOT_ELIGIBLE} and
 * {@code EXAM_TYPE_NOT_ALLOWED} leave {@code completeness} empty, so the missing list is not built.
 * {@code REQUIRED_DOCUMENTS_MISSING} carries that list and does not change the binding.
 */
public record SubmitCompletenessResult(SubmitCompleteness completeness, String error) {

    public static final String REQUIRED_DOCUMENTS_MISSING = "REQUIRED_DOCUMENTS_MISSING";
    public static final String EXAM_TYPE_NOT_ALLOWED = "EXAM_TYPE_NOT_ALLOWED";
    public static final String PROGRAM_NOT_ELIGIBLE = "PROGRAM_NOT_ELIGIBLE";

    public SubmitCompletenessResult {
        boolean early = PROGRAM_NOT_ELIGIBLE.equals(error) || EXAM_TYPE_NOT_ALLOWED.equals(error);
        boolean missing = REQUIRED_DOCUMENTS_MISSING.equals(error);
        if (error != null && !early && !missing) {
            throw new IllegalArgumentException("unsupported completeness error");
        }
        if (early) {
            if (completeness != null) {
                throw new IllegalArgumentException("missing list is not built");
            }
        } else if (completeness == null) {
            throw new IllegalArgumentException("completeness is required");
        } else if (missing == completeness.missingDocumentTypes().isEmpty()) {
            throw new IllegalArgumentException("missing documents do not match the error");
        }
    }

    public static SubmitCompletenessResult complete(SubmitCompleteness completeness) {
        return new SubmitCompletenessResult(completeness, null);
    }

    public static SubmitCompletenessResult missing(SubmitCompleteness completeness) {
        return new SubmitCompletenessResult(completeness, REQUIRED_DOCUMENTS_MISSING);
    }

    public static SubmitCompletenessResult programNotEligible() {
        return new SubmitCompletenessResult(null, PROGRAM_NOT_ELIGIBLE);
    }

    public static SubmitCompletenessResult examTypeNotAllowed() {
        return new SubmitCompletenessResult(null, EXAM_TYPE_NOT_ALLOWED);
    }
}

package mx.uam.sapcyti.academic.domain.model;

/**
 * Query result of {@code StudentExamContextPort}. {@code context} is present only when {@code outcome} is
 * {@link StudentExamContextOutcome#RESOLVED}.
 */
public record StudentExamContextResult(StudentExamContextOutcome outcome, StudentExamContext context) {

    public StudentExamContextResult {
        if (outcome == null) {
            throw new IllegalArgumentException("outcome is required");
        }
        if (outcome == StudentExamContextOutcome.RESOLVED && context == null) {
            throw new IllegalArgumentException("RESOLVED requires a context");
        }
        if (outcome != StudentExamContextOutcome.RESOLVED && context != null) {
            throw new IllegalArgumentException("context is present only when outcome is RESOLVED");
        }
    }

    public static StudentExamContextResult resolved(StudentExamContext context) {
        return new StudentExamContextResult(StudentExamContextOutcome.RESOLVED, context);
    }

    public static StudentExamContextResult of(StudentExamContextOutcome outcome) {
        return new StudentExamContextResult(outcome, null);
    }
}

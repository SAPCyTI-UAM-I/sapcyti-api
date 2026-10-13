package mx.uam.sapcyti.academic.domain.model;

/**
 * Result of resolving the single student-program row Exam Requests may use (HU-62).
 * BC-07 maps these outcomes to its own HTTP codes.
 */
public enum StudentExamContextOutcome {
    RESOLVED,
    NOT_FOUND,
    AMBIGUOUS,
    INCOMPLETE,
    DEGREE_UNSUPPORTED,
    STATUS_UNSUPPORTED
}

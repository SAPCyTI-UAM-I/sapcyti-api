package mx.uam.sapcyti.exam.domain.exception;

/**
 * A replace or delete was asked of a request whose status is not {@code BORRADOR}.
 * The current reference and the version stay unchanged.
 */
public class ExamRequestNotDraftException extends RuntimeException {

    public ExamRequestNotDraftException() {
        super("Exam request is not a draft");
    }
}

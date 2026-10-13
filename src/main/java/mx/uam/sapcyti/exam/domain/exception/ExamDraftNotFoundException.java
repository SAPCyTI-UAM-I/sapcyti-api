package mx.uam.sapcyti.exam.domain.exception;

/**
 * A save or delete found no request. The call does not insert a draft.
 */
public class ExamDraftNotFoundException extends RuntimeException {

    public ExamDraftNotFoundException() {
        super("Exam request was not found");
    }
}

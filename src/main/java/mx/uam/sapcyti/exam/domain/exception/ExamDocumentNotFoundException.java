package mx.uam.sapcyti.exam.domain.exception;

/**
 * A delete found no current document of that type. The version stays unchanged.
 */
public class ExamDocumentNotFoundException extends RuntimeException {

    public ExamDocumentNotFoundException() {
        super("Current exam document was not found");
    }
}

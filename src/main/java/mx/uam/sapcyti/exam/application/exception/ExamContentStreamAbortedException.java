package mx.uam.sapcyti.exam.application.exception;

/**
 * The PDF stream failed after the 200 headers were committed.
 * The handler must not replace that response with JSON or another status.
 */
public class ExamContentStreamAbortedException extends RuntimeException {

    public ExamContentStreamAbortedException() {
        super("Document stream aborted");
    }
}

package mx.uam.sapcyti.exam.domain.exception;

/**
 * The requested version is not the version stored on the draft.
 * References and superseded marks stay unchanged.
 */
public class StaleExamDraftException extends RuntimeException {

    private final int storedVersion;

    public StaleExamDraftException(int storedVersion) {
        super("Exam draft version conflict");
        this.storedVersion = storedVersion;
    }

    public int storedVersion() {
        return storedVersion;
    }
}

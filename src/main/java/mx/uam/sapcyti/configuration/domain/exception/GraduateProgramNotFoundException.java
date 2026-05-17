package mx.uam.sapcyti.configuration.domain.exception;

/**
 * Thrown when a graduate program cannot be found by id.
 */
public class GraduateProgramNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Graduate program not found";

    public GraduateProgramNotFoundException() {
        super(MESSAGE);
    }

    public GraduateProgramNotFoundException(Long id) {
        super(MESSAGE + " (id=" + id + ")");
    }
}

package mx.uam.sapcyti.configuration.domain.exception;

/**
 * Thrown when a graduate program name already exists.
 */
public class DuplicateGraduateProgramNameException extends RuntimeException {

    public static final String MESSAGE =
        "A graduate program with that name already exists";

    public DuplicateGraduateProgramNameException() {
        super(MESSAGE);
    }
}

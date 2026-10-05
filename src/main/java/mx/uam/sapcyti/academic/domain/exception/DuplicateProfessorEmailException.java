package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when registering a professor with an email already used by a User (HU-21).
 */
public class DuplicateProfessorEmailException extends RuntimeException {

    public static final String MESSAGE = "A user with this email already exists";

    public DuplicateProfessorEmailException() {
        super(MESSAGE);
    }
}

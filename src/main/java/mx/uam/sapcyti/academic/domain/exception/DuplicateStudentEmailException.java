package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when registering a student with an email already used by a User (HU-15).
 */
public class DuplicateStudentEmailException extends RuntimeException {

    public static final String MESSAGE = "A user with this email already exists";

    public DuplicateStudentEmailException() {
        super(MESSAGE);
    }
}

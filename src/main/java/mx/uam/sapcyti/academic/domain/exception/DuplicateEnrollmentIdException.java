package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when registering a student with a duplicate enrollment ID (HU-15).
 */
public class DuplicateEnrollmentIdException extends RuntimeException {

    public static final String MESSAGE = "A student with this enrollment ID already exists";

    public DuplicateEnrollmentIdException() {
        super(MESSAGE);
    }
}

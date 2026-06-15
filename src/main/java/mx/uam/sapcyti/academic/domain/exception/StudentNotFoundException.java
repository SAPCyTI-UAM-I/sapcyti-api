package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when a student cannot be found or is not visible in the current tenant (HU-15).
 */
public class StudentNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Student not found";

    public StudentNotFoundException() {
        super(MESSAGE);
    }
}

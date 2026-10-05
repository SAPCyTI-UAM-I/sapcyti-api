package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when a student program cannot be found in the current tenant scope (HU-19, HU-20).
 */
public class StudentProgramNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Student program not found";

    public StudentProgramNotFoundException() {
        super(MESSAGE);
    }
}

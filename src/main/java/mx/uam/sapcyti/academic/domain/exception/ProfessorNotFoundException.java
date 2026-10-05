package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when a student registration references a non-existent advisor (HU-15).
 */
public class ProfessorNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Professor not found";

    public ProfessorNotFoundException() {
        super(MESSAGE);
    }
}

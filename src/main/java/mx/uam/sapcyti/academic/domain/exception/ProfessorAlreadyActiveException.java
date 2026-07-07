package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when restoring a professor who is already active (HU-54).
 */
public class ProfessorAlreadyActiveException extends RuntimeException {

    public static final String ERROR_CODE = "PROFESSOR_ALREADY_ACTIVE";
    public static final String MESSAGE = "El profesor ya está activo.";

    public ProfessorAlreadyActiveException() {
        super(MESSAGE);
    }
}

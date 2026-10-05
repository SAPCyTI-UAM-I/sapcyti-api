package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when an update attempts to change or clear an assigned employee number (HU-24).
 */
public class EmployeeNumberImmutableException extends RuntimeException {

    public static final String ERROR_CODE = "NEMP_IMMUTABLE";
    public static final String MESSAGE = "El número económico no puede modificarse.";

    public EmployeeNumberImmutableException() {
        super(MESSAGE);
    }
}

package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when an update attempts to change professor type from INTERNO to EXTERNO (HU-24).
 */
public class InvalidTypeChangeException extends RuntimeException {

    public static final String ERROR_CODE = "INVALID_TYPE_CHANGE";
    public static final String MESSAGE = "Un profesor interno no puede cambiar a externo.";

    public InvalidTypeChangeException() {
        super(MESSAGE);
    }
}

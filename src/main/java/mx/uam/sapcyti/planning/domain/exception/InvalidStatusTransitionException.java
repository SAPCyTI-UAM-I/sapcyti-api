package mx.uam.sapcyti.planning.domain.exception;

public class InvalidStatusTransitionException extends RuntimeException {

    public static final String ERROR_CODE = "INVALID_STATUS_TRANSITION";
    public static final String MESSAGE = "Transición de estado no permitida.";

    public InvalidStatusTransitionException() {
        super(MESSAGE);
    }
}

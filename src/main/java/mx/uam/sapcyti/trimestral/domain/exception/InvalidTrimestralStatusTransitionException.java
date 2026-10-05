package mx.uam.sapcyti.trimestral.domain.exception;

public class InvalidTrimestralStatusTransitionException extends RuntimeException {

    public static final String ERROR_CODE = "INVALID_STATUS_TRANSITION";
    public static final String MESSAGE = "Transición de estado no válida.";

    public InvalidTrimestralStatusTransitionException() {
        super(MESSAGE);
    }
}

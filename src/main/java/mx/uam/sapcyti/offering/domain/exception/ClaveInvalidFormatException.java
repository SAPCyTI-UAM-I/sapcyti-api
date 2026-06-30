package mx.uam.sapcyti.offering.domain.exception;

public class ClaveInvalidFormatException extends RuntimeException {

    public static final String ERROR_CODE = "CLAVE_INVALID_FORMAT";
    public static final String MESSAGE = "La clave debe contener solo dígitos";

    public ClaveInvalidFormatException() {
        super(MESSAGE);
    }
}

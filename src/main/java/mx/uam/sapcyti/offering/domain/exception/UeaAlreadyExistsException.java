package mx.uam.sapcyti.offering.domain.exception;

public class UeaAlreadyExistsException extends RuntimeException {

    public static final String ERROR_CODE = "UEA_ALREADY_EXISTS";
    public static final String MESSAGE =
            "A UEA with this clave already exists in this graduate program";

    public UeaAlreadyExistsException() {
        super(MESSAGE);
    }
}

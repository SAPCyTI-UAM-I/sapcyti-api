package mx.uam.sapcyti.offering.domain.exception;

/**
 * Thrown when deactivating a UEA that is already inactive (HU-48).
 */
public class UeaAlreadyInactiveException extends RuntimeException {

    public static final String ERROR_CODE = "UEA_ALREADY_INACTIVE";
    public static final String MESSAGE = "UEA is already inactive";

    public UeaAlreadyInactiveException() {
        super(MESSAGE);
    }
}

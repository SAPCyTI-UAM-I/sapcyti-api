package mx.uam.sapcyti.offering.domain.exception;

/**
 * Thrown when restoring a UEA that is already active (HU-55).
 */
public class UeaAlreadyActiveException extends RuntimeException {

    public static final String ERROR_CODE = "UEA_ALREADY_ACTIVE";
    public static final String MESSAGE = "UEA is already active";

    public UeaAlreadyActiveException() {
        super(MESSAGE);
    }
}

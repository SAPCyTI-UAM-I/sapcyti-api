package mx.uam.sapcyti.offering.domain.exception;

public class UeaNotFoundException extends RuntimeException {

    public static final String MESSAGE = "UEA not found";

    public UeaNotFoundException() {
        super(MESSAGE);
    }
}

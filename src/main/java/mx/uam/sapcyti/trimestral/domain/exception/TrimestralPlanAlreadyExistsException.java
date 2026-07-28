package mx.uam.sapcyti.trimestral.domain.exception;

public class TrimestralPlanAlreadyExistsException extends RuntimeException {

    public static final String ERROR_CODE = "TRIMESTRAL_PLAN_ALREADY_EXISTS";
    public static final String MESSAGE = "Ya existe una planeación para ese trimestre.";

    public TrimestralPlanAlreadyExistsException() {
        super(MESSAGE);
    }
}

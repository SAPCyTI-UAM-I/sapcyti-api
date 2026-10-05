package mx.uam.sapcyti.planning.domain.exception;

public class AnnualPlanAlreadyExistsException extends RuntimeException {

    public static final String ERROR_CODE = "ANNUAL_PLAN_ALREADY_EXISTS";
    public static final String MESSAGE = "Ya existe una planeación para este año.";

    public AnnualPlanAlreadyExistsException() {
        super(MESSAGE);
    }
}

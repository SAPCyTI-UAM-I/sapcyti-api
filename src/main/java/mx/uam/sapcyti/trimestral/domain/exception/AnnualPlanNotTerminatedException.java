package mx.uam.sapcyti.trimestral.domain.exception;

public class AnnualPlanNotTerminatedException extends RuntimeException {

    public static final String ERROR_CODE = "ANNUAL_PLAN_NOT_TERMINATED";
    public static final String MESSAGE = "The annual plan must be terminated";

    public AnnualPlanNotTerminatedException() {
        super(MESSAGE);
    }
}

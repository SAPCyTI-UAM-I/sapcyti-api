package mx.uam.sapcyti.trimestral.domain.exception;

public class AnnualPlanRequiredException extends RuntimeException {

    public static final String ERROR_CODE = "ANNUAL_PLAN_REQUIRED";
    public static final String MESSAGE =
            "No hay planeación anual para el año de ese trimestre; créala primero.";

    public AnnualPlanRequiredException() {
        super(MESSAGE);
    }
}

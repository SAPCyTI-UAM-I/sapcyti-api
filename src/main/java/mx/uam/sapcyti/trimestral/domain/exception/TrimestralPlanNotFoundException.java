package mx.uam.sapcyti.trimestral.domain.exception;

public class TrimestralPlanNotFoundException extends RuntimeException {

    public static final String ERROR_CODE = "TRIMESTRAL_PLAN_NOT_FOUND";
    public static final String MESSAGE = "No se encontró la planeación trimestral.";

    public TrimestralPlanNotFoundException() {
        super(MESSAGE);
    }
}

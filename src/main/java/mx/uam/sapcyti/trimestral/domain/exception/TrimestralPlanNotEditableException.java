package mx.uam.sapcyti.trimestral.domain.exception;

public class TrimestralPlanNotEditableException extends RuntimeException {

    public static final String ERROR_CODE = "TRIMESTRAL_PLAN_NOT_EDITABLE";
    public static final String MESSAGE =
            "La planeación terminada no puede editarse; regrésala a borrador.";

    public TrimestralPlanNotEditableException() {
        super(MESSAGE);
    }
}

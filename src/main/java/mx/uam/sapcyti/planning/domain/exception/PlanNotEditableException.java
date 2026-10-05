package mx.uam.sapcyti.planning.domain.exception;

public class PlanNotEditableException extends RuntimeException {

    public static final String ERROR_CODE = "PLAN_NOT_EDITABLE";
    public static final String MESSAGE = "Solo se puede editar una planeación en Borrador.";

    public PlanNotEditableException() {
        super(MESSAGE);
    }
}

package mx.uam.sapcyti.survey.domain.exception;

public class BlankWithUeasConflictException extends RuntimeException {

    public static final String ERROR_CODE = "BLANK_WITH_UEAS_CONFLICT";
    public static final String MESSAGE = "No puedes seleccionar UEAs junto con una inscripción en blanco.";

    public BlankWithUeasConflictException() {
        super(MESSAGE);
    }
}

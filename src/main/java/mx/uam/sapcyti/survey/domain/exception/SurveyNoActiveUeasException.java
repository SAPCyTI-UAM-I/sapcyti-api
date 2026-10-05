package mx.uam.sapcyti.survey.domain.exception;

public class SurveyNoActiveUeasException extends RuntimeException {

    public static final String ERROR_CODE = "SURVEY_NO_ACTIVE_UEAS";
    public static final String MESSAGE = "No hay UEAs activas en el catálogo; no se puede publicar el sondeo.";

    public SurveyNoActiveUeasException() {
        super(MESSAGE);
    }
}

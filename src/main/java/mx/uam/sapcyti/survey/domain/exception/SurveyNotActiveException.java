package mx.uam.sapcyti.survey.domain.exception;

public class SurveyNotActiveException extends RuntimeException {

    public static final String ERROR_CODE = "SURVEY_NOT_ACTIVE";
    public static final String MESSAGE = "El sondeo no está activo.";

    public SurveyNotActiveException() {
        super(MESSAGE);
    }
}

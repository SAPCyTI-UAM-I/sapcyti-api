package mx.uam.sapcyti.survey.domain.exception;

public class SurveyNotDeletableException extends RuntimeException {

    public static final String ERROR_CODE = "SURVEY_NOT_DELETABLE";
    public static final String MESSAGE = "Solo puede eliminarse un sondeo programado sin respuestas.";

    public SurveyNotDeletableException() {
        super(MESSAGE);
    }
}

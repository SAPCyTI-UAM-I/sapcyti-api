package mx.uam.sapcyti.survey.domain.exception;

public class SurveyNotClosedException extends RuntimeException {

    public static final String ERROR_CODE = "SURVEY_NOT_CLOSED";
    public static final String MESSAGE = "La planeación solo se genera desde una encuesta cerrada.";

    public SurveyNotClosedException() {
        super(MESSAGE);
    }
}

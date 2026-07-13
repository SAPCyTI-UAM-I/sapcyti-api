package mx.uam.sapcyti.survey.domain.exception;

public class SurveyNotFoundException extends RuntimeException {

    public static final String ERROR_CODE = "SURVEY_NOT_FOUND";
    public static final String MESSAGE = "No existe un sondeo para ese trimestre.";

    public SurveyNotFoundException() {
        super(MESSAGE);
    }
}

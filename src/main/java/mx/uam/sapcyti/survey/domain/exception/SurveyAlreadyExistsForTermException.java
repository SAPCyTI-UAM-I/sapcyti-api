package mx.uam.sapcyti.survey.domain.exception;

public class SurveyAlreadyExistsForTermException extends RuntimeException {

    public static final String ERROR_CODE = "SURVEY_ALREADY_EXISTS_FOR_TERM";
    public static final String MESSAGE = "Ya existe un sondeo para ese trimestre.";

    public SurveyAlreadyExistsForTermException() {
        super(MESSAGE);
    }
}

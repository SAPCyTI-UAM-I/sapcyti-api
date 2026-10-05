package mx.uam.sapcyti.survey.domain.exception;

public class SurveyWindowOverlapException extends RuntimeException {

    public static final String ERROR_CODE = "SURVEY_WINDOW_OVERLAPS";
    public static final String MESSAGE = "Ya existe un sondeo activo o programado en ese periodo.";

    public SurveyWindowOverlapException() {
        super(MESSAGE);
    }
}

package mx.uam.sapcyti.survey.domain.exception;

public class SurveyReopenDatesInvalidException extends RuntimeException {

    public static final String ERROR_CODE = "SURVEY_REOPEN_DATES_INVALID";
    public static final String MESSAGE =
            "Para reabrir el sondeo, la fecha de apertura y la de cierre deben ser futuras.";

    public SurveyReopenDatesInvalidException() {
        super(MESSAGE);
    }
}

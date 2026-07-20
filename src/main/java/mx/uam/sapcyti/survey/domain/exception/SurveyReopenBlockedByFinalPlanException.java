package mx.uam.sapcyti.survey.domain.exception;

public class SurveyReopenBlockedByFinalPlanException extends RuntimeException {

    public static final String ERROR_CODE = "SURVEY_REOPEN_BLOCKED_TERMINATED_PLAN";
    public static final String MESSAGE =
            "No se puede reabrir la encuesta: existe una planeación trimestral terminada para ese trimestre.";

    public SurveyReopenBlockedByFinalPlanException() {
        super(MESSAGE);
    }
}

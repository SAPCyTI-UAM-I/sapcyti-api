package mx.uam.sapcyti.survey.domain.exception;

public class StudentSurveyResponseNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Student has not submitted a response for this survey";

    public StudentSurveyResponseNotFoundException() {
        super(MESSAGE);
    }
}

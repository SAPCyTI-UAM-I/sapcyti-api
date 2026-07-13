package mx.uam.sapcyti.offering.domain.exception;

public class UeaInActiveSurveyException extends RuntimeException {

    public static final String ERROR_CODE = "UEA_IN_ACTIVE_SURVEY";

    private final String term;

    public UeaInActiveSurveyException(String term) {
        super("La UEA está incluida en el sondeo activo del trimestre " + term + ".");
        this.term = term;
    }

    public String getTerm() {
        return term;
    }
}

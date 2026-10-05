package mx.uam.sapcyti.planning.domain.exception;

public class AnnualPlanNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Annual plan not found for this graduate program";

    public AnnualPlanNotFoundException() {
        super(MESSAGE);
    }
}

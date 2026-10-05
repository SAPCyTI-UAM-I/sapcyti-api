package mx.uam.sapcyti.planning.domain.exception;

public class AnnualPlanEntryNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Annual plan entry not found in this plan";

    public AnnualPlanEntryNotFoundException() {
        super(MESSAGE);
    }
}

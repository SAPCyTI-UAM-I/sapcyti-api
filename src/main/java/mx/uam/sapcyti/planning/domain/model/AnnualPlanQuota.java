package mx.uam.sapcyti.planning.domain.model;

/**
 * Thin quota projection for trimestral generation — avoids importing the AnnualPlan aggregate.
 */
public record AnnualPlanQuota(Long ueaId, String cupoI, String cupoP, String cupoO) {

    public String cupoForTrimester(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'I' -> cupoI;
            case 'P' -> cupoP;
            case 'O' -> cupoO;
            default -> null;
        };
    }
}

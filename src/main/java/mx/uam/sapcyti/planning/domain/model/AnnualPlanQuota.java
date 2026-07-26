package mx.uam.sapcyti.planning.domain.model;

/**
 * Thin quota projection for trimestral generation — avoids importing the AnnualPlan aggregate.
 */
public record AnnualPlanQuota(
        Long ueaId,
        String gruposI,
        String cupoI,
        String gruposP,
        String cupoP,
        String gruposO,
        String cupoO) {

    public String groupsForTrimester(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'I' -> gruposI;
            case 'P' -> gruposP;
            case 'O' -> gruposO;
            default -> null;
        };
    }

    public String cupoForTrimester(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'I' -> cupoI;
            case 'P' -> cupoP;
            case 'O' -> cupoO;
            default -> null;
        };
    }
}

package mx.uam.sapcyti.survey.domain.service;

import java.util.Optional;

public final class SuggestedTermCalculator {

    private SuggestedTermCalculator() {
    }

    /**
     * Rotates O → I → P → O; increments year on P → O.
     */
    public static Optional<String> suggestNext(String latestTerm) {
        if (latestTerm == null || latestTerm.length() != 3) {
            return Optional.empty();
        }
        String yearPart = latestTerm.substring(0, 2);
        char period = latestTerm.charAt(2);
        int year = Integer.parseInt(yearPart);

        return switch (period) {
            case 'O' -> Optional.of(yearPart + "I");
            case 'I' -> Optional.of(yearPart + "P");
            case 'P' -> Optional.of(String.format("%02d", (year + 1) % 100) + "O");
            default -> Optional.empty();
        };
    }
}

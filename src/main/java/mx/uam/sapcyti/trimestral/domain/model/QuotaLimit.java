package mx.uam.sapcyti.trimestral.domain.model;

/**
 * Interprets the annual-plan quota notation used by trimestral planning.
 *
 * <p>{@code *} and absent values are unbounded. Format validation remains in the owning
 * aggregate; this type only converts an already validated value into a finite limit.
 */
public final class QuotaLimit {

    private QuotaLimit() {
    }

    public static Integer finiteValue(String value) {
        if (value == null || value.isBlank() || "*".equals(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

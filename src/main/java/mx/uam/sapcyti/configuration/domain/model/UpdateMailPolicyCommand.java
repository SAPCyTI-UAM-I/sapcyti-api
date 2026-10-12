package mx.uam.sapcyti.configuration.domain.model;

/**
 * Command to change the global mail mode (HU-35).
 *
 * <p>Source of truth:
 * {@code program-configuration.schema.json#/definitions/UpdateMailPolicyCommand}.
 * Only mode and the version the caller last observed. The same mode with the
 * current version is a no-op. Any other version is a conflict.
 *
 * @param mode    requested mode
 * @param version version observed by the caller; must be &gt;= 0
 */
public record UpdateMailPolicyCommand(MailMode mode, int version) {

    public UpdateMailPolicyCommand {
        if (mode == null) {
            throw new IllegalArgumentException("Mail mode is required");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Mail policy version must be >= 0");
        }
    }
}

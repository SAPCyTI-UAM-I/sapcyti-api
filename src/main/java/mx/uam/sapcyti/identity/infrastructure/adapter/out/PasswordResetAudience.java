package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import mx.uam.sapcyti.configuration.domain.model.MailAudience;
import mx.uam.sapcyti.identity.domain.model.RoleType;

/**
 * Audience declared for a password-reset account (SPEC-043).
 * The gate does not infer it from the address.
 */
final class PasswordResetAudience {

    private PasswordResetAudience() {
    }

    /**
     * @return {@link MailAudience#STUDENT} or {@link MailAudience#STAFF}, or null
     *         when the role must not call the provider
     */
    static MailAudience of(RoleType role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case STUDENT -> MailAudience.STUDENT;
            case PROFESSOR, COORDINATOR -> MailAudience.STAFF;
            case SYSTEM_ADMIN, ASSISTANT, SPEAKER -> null;
        };
    }
}

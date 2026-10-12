package mx.uam.sapcyti.identity.domain.port.out;

import java.util.Locale;
import mx.uam.sapcyti.identity.domain.model.RoleType;

/**
 * Outbound email delivery (HU-02 password recovery, SPEC-015).
 *
 * <p>{@link #sendPasswordReset} is the identity port. Its adapters deliver only
 * through the mail dispatch gate (SPEC-043). The account role selects the audience.
 * An unclassifiable role does not call the provider.
 */
public interface EmailPort {

    /**
     * Sends a password-reset message for {@code role}.
     *
     * @param toEmail  account address; the gate may replace the envelope
     * @param rawToken plaintext token placed in the reset link
     * @param locale   message language; null defaults to Spanish
     * @param role     account role used to declare the audience
     */
    void sendPasswordReset(String toEmail, String rawToken, Locale locale, RoleType role);
}

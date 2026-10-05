package mx.uam.sapcyti.identity.domain.port.out;

import java.util.Locale;

/**
 * Outbound email delivery (HU-02 password recovery, SPEC-015).
 */
public interface EmailPort {
    void sendPasswordReset(String toEmail, String rawToken, Locale locale);
}

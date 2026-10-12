package mx.uam.sapcyti.configuration.domain.exception;

/**
 * The authoritative mail-policy row or store cannot be used (SPEC-041).
 * Callers must not substitute an assumed mode.
 */
public class MailPolicyUnavailableException extends RuntimeException {

    public static final String MESSAGE = "Mail policy is temporarily unavailable";

    public MailPolicyUnavailableException() {
        super(MESSAGE);
    }
}

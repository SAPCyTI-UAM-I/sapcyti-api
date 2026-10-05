package mx.uam.sapcyti.identity.domain.exception;

/**
 * Thrown when a password reset token has passed its TTL (HU-02).
 */
public class ExpiredResetTokenException extends RuntimeException {

    public static final String MESSAGE = "The reset token has expired. Please request a new one";

    public ExpiredResetTokenException() {
        super(MESSAGE);
    }
}

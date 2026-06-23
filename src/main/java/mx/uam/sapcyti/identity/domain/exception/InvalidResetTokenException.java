package mx.uam.sapcyti.identity.domain.exception;

/**
 * Thrown when a password reset token does not match any stored hash (HU-02).
 */
public class InvalidResetTokenException extends RuntimeException {

    public static final String MESSAGE = "Invalid reset token";

    public InvalidResetTokenException() {
        super(MESSAGE);
    }
}

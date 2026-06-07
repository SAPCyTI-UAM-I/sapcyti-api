package mx.uam.sapcyti.identity.domain.exception;

/**
 * Thrown when a password reset token has already been consumed (HU-02).
 */
public class UsedResetTokenException extends RuntimeException {

    public static final String MESSAGE = "This reset token has already been used";

    public UsedResetTokenException() {
        super(MESSAGE);
    }
}

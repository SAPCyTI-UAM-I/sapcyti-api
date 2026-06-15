package mx.uam.sapcyti.identity.domain.exception;

/**
 * Thrown when self-change password is attempted with a wrong current password (HU-28).
 */
public class IncorrectCurrentPasswordException extends RuntimeException {

    public static final String MESSAGE = "Current password is incorrect";

    public IncorrectCurrentPasswordException() {
        super(MESSAGE);
    }
}

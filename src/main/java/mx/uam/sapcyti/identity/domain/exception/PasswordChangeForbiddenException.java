package mx.uam.sapcyti.identity.domain.exception;

/**
 * Thrown when an authenticated user attempts to change another user's password without permission (HU-28).
 */
public class PasswordChangeForbiddenException extends RuntimeException {

    public static final String MESSAGE = "You can only change your own password";

    public PasswordChangeForbiddenException() {
        super(MESSAGE);
    }
}

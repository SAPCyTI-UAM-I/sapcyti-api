package mx.uam.sapcyti.identity.domain.exception;

/**
 * Thrown when email/password validation fails (SPEC-012 EC-1).
 */
public class InvalidCredentialsException extends RuntimeException {

    public static final String MESSAGE = "Invalid credentials";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }
}

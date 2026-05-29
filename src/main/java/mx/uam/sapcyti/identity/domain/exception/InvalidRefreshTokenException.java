package mx.uam.sapcyti.identity.domain.exception;

/**
 * Thrown when a refresh token is missing, expired, or revoked (SPEC-012 EC-2).
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public static final String MESSAGE = "Invalid refresh token";

    public InvalidRefreshTokenException() {
        super(MESSAGE);
    }
}

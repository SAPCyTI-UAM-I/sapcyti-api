package mx.uam.sapcyti.identity.domain.exception;

/**
 * Thrown when a user id does not exist (HU-28).
 */
public class UserNotFoundException extends RuntimeException {

    public static final String MESSAGE = "User not found";

    public UserNotFoundException() {
        super(MESSAGE);
    }
}

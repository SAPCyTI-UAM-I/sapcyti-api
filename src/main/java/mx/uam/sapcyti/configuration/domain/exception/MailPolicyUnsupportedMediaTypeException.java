package mx.uam.sapcyti.configuration.domain.exception;

/**
 * PUT /api/mail-policy did not use {@code application/json} (SPEC-041).
 * {@code charset=UTF-8} remains acceptable and does not raise this exception.
 */
public class MailPolicyUnsupportedMediaTypeException extends RuntimeException {

    public static final String MESSAGE = "Content-Type must be application/json";

    public MailPolicyUnsupportedMediaTypeException() {
        super(MESSAGE);
    }
}

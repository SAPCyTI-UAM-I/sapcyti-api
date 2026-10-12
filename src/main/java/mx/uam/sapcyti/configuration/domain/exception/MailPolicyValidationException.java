package mx.uam.sapcyti.configuration.domain.exception;

import java.util.List;

/**
 * The mail-policy request failed validation (SPEC-041).
 *
 * <p>The list is the errors of the first failing phase, in input order.
 */
public class MailPolicyValidationException extends RuntimeException {

    private final List<MailPolicyFieldError> fieldErrors;

    public MailPolicyValidationException(List<MailPolicyFieldError> fieldErrors) {
        super(summary(fieldErrors));
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<MailPolicyFieldError> fieldErrors() {
        return fieldErrors;
    }

    private static String summary(List<MailPolicyFieldError> fieldErrors) {
        if (fieldErrors.size() == 1
                && "mode".equals(fieldErrors.get(0).field())
                && "INVALID_ENUM".equals(fieldErrors.get(0).code())) {
            return "Invalid mail mode";
        }
        return "Invalid request";
    }
}

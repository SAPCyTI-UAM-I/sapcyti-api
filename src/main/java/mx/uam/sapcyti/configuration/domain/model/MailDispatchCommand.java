package mx.uam.sapcyti.configuration.domain.model;

/**
 * One dispatch attempt: the audience the caller declares and the recipients that
 * flow would use when the mode is {@link MailMode#ENABLED}.
 *
 * <p>A null audience is not {@link MailAudience#STUDENT} or {@link MailAudience#STAFF}
 * and the gate blocks it. The gate does not infer the audience from addresses.
 *
 * @param audience audience declared by the owning flow; null blocks dispatch
 * @param intended recipients of the owning flow
 */
public record MailDispatchCommand(MailAudience audience, MailEnvelope intended) {

    public MailDispatchCommand {
        if (intended == null) {
            throw new IllegalArgumentException("Intended mail envelope is required");
        }
    }
}

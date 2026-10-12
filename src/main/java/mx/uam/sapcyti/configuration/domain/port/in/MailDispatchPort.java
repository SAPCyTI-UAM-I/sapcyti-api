package mx.uam.sapcyti.configuration.domain.port.in;

import mx.uam.sapcyti.configuration.domain.model.MailDispatchCommand;
import mx.uam.sapcyti.configuration.domain.model.MailDispatchOutcome;
import mx.uam.sapcyti.configuration.domain.model.MailEnvelope;

/**
 * Dispatch gate in front of the mail provider (SPEC-042).
 *
 * <p>{@link #dispatch} is the only path that may call the provider. The sender
 * receives the envelope already resolved for the mode just read.
 */
public interface MailDispatchPort {

    /**
     * Reads the fresh policy under the shared read lock and applies it.
     *
     * @param command audience and intended recipients
     * @param sender  sends the resolved envelope; not called when the outcome is
     *                {@link MailDispatchOutcome#SUPPRESSED} or {@link MailDispatchOutcome#BLOCKED}
     * @return {@link MailDispatchOutcome#SENT} only when {@code sender} accepts
     */
    MailDispatchOutcome dispatch(MailDispatchCommand command, MailSender sender);

    /**
     * Sends one already resolved envelope. A normal return is provider acceptance.
     */
    @FunctionalInterface
    interface MailSender {
        void send(MailEnvelope envelope);
    }
}

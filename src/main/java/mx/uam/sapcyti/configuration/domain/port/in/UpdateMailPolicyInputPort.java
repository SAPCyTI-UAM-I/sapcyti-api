package mx.uam.sapcyti.configuration.domain.port.in;

import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.model.UpdateMailPolicyCommand;

/**
 * Input port for changing the single global mail policy (SPEC-041).
 * The actor comes from the authenticated JWT, not from the command.
 */
public interface UpdateMailPolicyInputPort {

    /**
     * @param command observed mode and version
     * @return the row after a change or a no-op
     * @throws mx.uam.sapcyti.configuration.domain.exception.MailPolicyVersionConflictException
     *     when {@code command.version()} is not the stored version
     * @throws mx.uam.sapcyti.configuration.domain.exception.MailPolicyUnavailableException
     *     when the row or the store cannot be used
     */
    MailPolicy update(UpdateMailPolicyCommand command);
}

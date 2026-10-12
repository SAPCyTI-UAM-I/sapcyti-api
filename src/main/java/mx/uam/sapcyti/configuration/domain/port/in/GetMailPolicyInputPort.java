package mx.uam.sapcyti.configuration.domain.port.in;

import mx.uam.sapcyti.configuration.domain.model.MailPolicy;

/**
 * Input port for reading the single global mail policy (SPEC-041).
 */
public interface GetMailPolicyInputPort {

    /**
     * @return the authoritative row
     * @throws mx.uam.sapcyti.configuration.domain.exception.MailPolicyUnavailableException
     *     when the row or the store cannot be read
     */
    MailPolicy get();
}

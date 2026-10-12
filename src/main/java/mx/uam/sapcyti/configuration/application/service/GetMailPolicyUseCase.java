package mx.uam.sapcyti.configuration.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.configuration.domain.exception.MailPolicyUnavailableException;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.port.in.GetMailPolicyInputPort;
import mx.uam.sapcyti.configuration.domain.port.out.MailPolicyRepositoryPort;

/**
 * Reads the single global mail policy (SPEC-041).
 */
@Service
public class GetMailPolicyUseCase implements GetMailPolicyInputPort {

    private static final Logger LOG = LoggerFactory.getLogger(GetMailPolicyUseCase.class);

    private final MailPolicyRepositoryPort policies;

    public GetMailPolicyUseCase(MailPolicyRepositoryPort policies) {
        this.policies = policies;
    }

    @Override
    @Transactional(readOnly = true)
    public MailPolicy get() {
        try {
            return policies.load();
        } catch (IllegalStateException | DataAccessException ex) {
            LOG.warn("Mail policy could not be read", ex);
            throw new MailPolicyUnavailableException();
        }
    }
}

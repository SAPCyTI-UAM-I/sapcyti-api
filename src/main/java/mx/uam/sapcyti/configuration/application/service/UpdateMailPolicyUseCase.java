package mx.uam.sapcyti.configuration.application.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import mx.uam.sapcyti.configuration.domain.exception.MailPolicyUnavailableException;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.model.UpdateMailPolicyCommand;
import mx.uam.sapcyti.configuration.domain.port.in.UpdateMailPolicyInputPort;
import mx.uam.sapcyti.configuration.domain.port.out.MailPolicyRepositoryPort;
import mx.uam.sapcyti.identity.infrastructure.security.AuthenticatedUserResolver;

/**
 * Changes the single global mail policy (SPEC-041, SPEC-042).
 *
 * <p>The actor is the authenticated user. The same mode and current version is a
 * no-op delegated to {@link MailPolicyRepositoryPort#compareAndSwap}. This use
 * case does not send mail.
 *
 * <p>The write lock is acquired before {@code compareAndSwap}. That call owns the
 * short SQL transaction and commits before the lock is released, so the
 * transaction is not open while a dispatch still holds the read lock.
 */
@Service
public class UpdateMailPolicyUseCase implements UpdateMailPolicyInputPort {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateMailPolicyUseCase.class);

    private final MailPolicyRepositoryPort policies;
    private final AuthenticatedUserResolver users;
    private final MailDispatchGateService gate;

    public UpdateMailPolicyUseCase(
            MailPolicyRepositoryPort policies,
            AuthenticatedUserResolver users,
            MailDispatchGateService gate) {
        this.policies = policies;
        this.users = users;
        this.gate = gate;
    }

    @Override
    public MailPolicy update(UpdateMailPolicyCommand command) {
        Long actorUserId = users.resolve().getUserId();
        return gate.withWriteLock(() -> compareAndSwap(command, actorUserId));
    }

    private MailPolicy compareAndSwap(UpdateMailPolicyCommand command, Long actorUserId) {
        try {
            return policies.compareAndSwap(command, actorUserId, Instant.now());
        } catch (IllegalStateException | DataAccessException ex) {
            LOG.warn("Mail policy could not be updated", ex);
            throw new MailPolicyUnavailableException();
        }
    }
}

package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import java.time.Instant;
import mx.uam.sapcyti.configuration.domain.exception.MailPolicyVersionConflictException;
import mx.uam.sapcyti.configuration.domain.model.MailMode;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.model.MailPolicyAuditEntry;
import mx.uam.sapcyti.configuration.domain.model.UpdateMailPolicyCommand;
import mx.uam.sapcyti.configuration.domain.port.out.MailPolicyRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for {@link MailPolicyRepositoryPort}.
 *
 * <p>The singleton row is locked, then the mode change and the audit insert
 * commit or roll back together. A failure while writing the audit leaves the
 * previous mode in place.
 */
@Repository
public class MailPolicyJpaAdapter implements MailPolicyRepositoryPort {

    private final SpringDataMailPolicyRepository policies;
    private final SpringDataMailPolicyAuditRepository auditEntries;

    public MailPolicyJpaAdapter(
            SpringDataMailPolicyRepository policies,
            SpringDataMailPolicyAuditRepository auditEntries) {
        this.policies = policies;
        this.auditEntries = auditEntries;
    }

    @Override
    @Transactional(readOnly = true)
    public MailPolicy load() {
        return policies.findById(MailPolicy.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Mail policy row is missing"));
    }

    @Override
    @Transactional
    public MailPolicy compareAndSwap(
            UpdateMailPolicyCommand command,
            Long actorUserId,
            Instant occurredAt) {
        if (command == null) {
            throw new IllegalArgumentException("UpdateMailPolicyCommand is required");
        }
        MailPolicy current = policies.findByIdForUpdate(MailPolicy.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Mail policy row is missing"));
        if (current.getVersion() != command.version()) {
            throw new MailPolicyVersionConflictException(current.getVersion());
        }
        if (current.getMode() == command.mode()) {
            return current;
        }
        MailMode previousMode = current.getMode();
        int previousVersion = current.getVersion();
        current.changeMode(command.mode(), actorUserId, occurredAt);
        policies.flush();
        auditEntries.saveAndFlush(new MailPolicyAuditEntry(
                previousMode,
                current.getMode(),
                previousVersion,
                current.getVersion(),
                actorUserId,
                occurredAt));
        return current;
    }
}

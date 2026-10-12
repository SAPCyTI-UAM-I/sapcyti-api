package mx.uam.sapcyti.configuration.domain.port.out;

import java.time.Instant;
import mx.uam.sapcyti.configuration.domain.exception.MailPolicyVersionConflictException;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.model.UpdateMailPolicyCommand;

/**
 * Persistence port for the single global {@link MailPolicy} row (HU-35, SPEC-040).
 *
 * <p>{@link #load} reads that row. {@link #compareAndSwap} confirms a mode change
 * and its audit entry together, or rolls both back. The same mode with the current
 * version does not increment version and does not write an audit entry.
 */
public interface MailPolicyRepositoryPort {

    /**
     * Loads the singleton row created by the mail-policy migration.
     *
     * @return the current policy
     * @throws IllegalStateException if the row is missing
     */
    MailPolicy load();

    /**
     * Compare-and-swap of the global mode.
     *
     * <p>When {@code command.version()} is current and the mode differs, the row
     * stores the new mode, previous version + 1, the actor and UTC, and one
     * {@code MailPolicyAuditEntry}. When the mode and version are already current,
     * the row and the audit history stay as they are. When the version is not
     * current, including a higher one, nothing is written.
     *
     * @param command      observed mode and version
     * @param actorUserId  actor of an effective change; references users(id)
     * @param occurredAt   UTC of an effective change
     * @return the row after the call; unchanged on a no-op
     * @throws MailPolicyVersionConflictException when the version is not current
     */
    MailPolicy compareAndSwap(
            UpdateMailPolicyCommand command,
            Long actorUserId,
            Instant occurredAt);
}

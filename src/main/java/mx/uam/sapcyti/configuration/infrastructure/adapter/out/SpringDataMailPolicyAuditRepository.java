package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import mx.uam.sapcyti.configuration.domain.model.MailPolicyAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link MailPolicyAuditEntry}. Internal to the adapter.
 */
interface SpringDataMailPolicyAuditRepository
        extends JpaRepository<MailPolicyAuditEntry, Long> {
}

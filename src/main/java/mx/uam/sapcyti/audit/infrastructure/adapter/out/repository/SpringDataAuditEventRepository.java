package mx.uam.sapcyti.audit.infrastructure.adapter.out.repository;

import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAuditEventRepository extends JpaRepository<AuditEvent, Long> {
}

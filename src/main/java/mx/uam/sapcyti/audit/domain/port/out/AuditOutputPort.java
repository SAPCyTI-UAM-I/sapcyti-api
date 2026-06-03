package mx.uam.sapcyti.audit.domain.port.out;

import mx.uam.sapcyti.audit.domain.model.AuditEvent;

public interface AuditOutputPort {
    void record(AuditEvent event);
}

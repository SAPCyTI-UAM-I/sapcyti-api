package mx.uam.sapcyti.audit.infrastructure.adapter.out;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.audit.infrastructure.adapter.out.repository.SpringDataAuditEventRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditJpaAdapter implements AuditOutputPort {

    private final SpringDataAuditEventRepository repository;

    @Override
    public void record(AuditEvent event) {
        repository.save(event);
    }
}

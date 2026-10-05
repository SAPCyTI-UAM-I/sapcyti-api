package mx.uam.sapcyti.offering.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUeasUseCase {

    private final UeaRepositoryPort ueaRepository;

    @Transactional(readOnly = true)
    public Page<UEA> execute(String search, Boolean active, Pageable pageable) {
        Long graduateProgramId = requireTenant();
        return ueaRepository.findByGraduateProgramId(graduateProgramId, search, active, pageable);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }
}

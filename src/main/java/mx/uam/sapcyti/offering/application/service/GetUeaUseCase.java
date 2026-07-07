package mx.uam.sapcyti.offering.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.exception.UeaNotFoundException;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-47 — loads a single UEA (edit view; refresh/deep-link safe). Tenant-scoped. */
@Service
@RequiredArgsConstructor
public class GetUeaUseCase {

    private final UeaRepositoryPort ueaRepository;

    @Transactional(readOnly = true)
    public UEA execute(Long ueaId) {
        Long graduateProgramId = requireTenant();
        return ueaRepository
                .findByIdAndGraduateProgramId(ueaId, graduateProgramId)
                .orElseThrow(UeaNotFoundException::new);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }
}

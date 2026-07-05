package mx.uam.sapcyti.offering.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.exception.UeaNotFoundException;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateUeaUseCase {

    private final UeaRepositoryPort ueaRepository;

    @Transactional
    public UEA execute(Long ueaId) {
        Long graduateProgramId = requireTenant();

        UEA uea = ueaRepository.findByIdAndGraduateProgramId(ueaId, graduateProgramId)
                .orElseThrow(UeaNotFoundException::new);

        uea.deactivate();

        return ueaRepository.save(uea);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }
}

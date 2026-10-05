package mx.uam.sapcyti.offering.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.exception.UeaAlreadyInactiveException;
import mx.uam.sapcyti.offering.domain.exception.UeaInActiveSurveyException;
import mx.uam.sapcyti.offering.domain.exception.UeaNotFoundException;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.SurveyActivityPort;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateUeaUseCase {

    private final UeaRepositoryPort ueaRepository;
    private final SurveyActivityPort surveyActivityPort;

    @Transactional
    public UEA execute(Long ueaId) {
        return execute(ueaId, false);
    }

    @Transactional
    public UEA execute(Long ueaId, boolean confirm) {
        Long graduateProgramId = requireTenant();

        UEA uea = ueaRepository.findByIdAndGraduateProgramId(ueaId, graduateProgramId)
                .orElseThrow(UeaNotFoundException::new);

        surveyActivityPort.findActiveSurveyTermIncluding(ueaId, graduateProgramId)
                .ifPresent(term -> {
                    if (!confirm) {
                        throw new UeaInActiveSurveyException(term);
                    }
                });

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

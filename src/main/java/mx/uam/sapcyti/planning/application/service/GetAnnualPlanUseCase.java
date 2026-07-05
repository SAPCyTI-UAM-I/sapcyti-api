package mx.uam.sapcyti.planning.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.planning.domain.exception.AnnualPlanNotFoundException;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanStatus;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.planning.domain.service.CatalogSyncService;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAnnualPlanUseCase {

    private final AnnualPlanRepositoryPort annualPlanRepository;
    private final UeaRepositoryPort ueaRepository;
    private final CatalogSyncService catalogSyncService;

    @Transactional
    public AnnualPlan execute(int year) {
        Long graduateProgramId = requireTenant();
        AnnualPlan plan = annualPlanRepository
                .findByYearAndGraduateProgramId(year, graduateProgramId)
                .orElseThrow(AnnualPlanNotFoundException::new);

        if (plan.getStatus() == AnnualPlanStatus.BORRADOR) {
            List<UEA> activeUeas = ueaRepository.findActiveByGraduateProgramId(graduateProgramId);
            catalogSyncService.sync(plan, activeUeas);
            plan = annualPlanRepository.save(plan);
        }

        return plan;
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }
}

package mx.uam.sapcyti.planning.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.domain.exception.AnnualPlanNotFoundException;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanStatus;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanChangePort;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeStatusUseCase {

    private final AnnualPlanRepositoryPort annualPlanRepository;
    private final AnnualPlanChangePort annualPlanChangePort;

    @Transactional
    public AnnualPlan execute(int year, AnnualPlanStatus newStatus) {
        Long graduateProgramId = requireTenant();
        AnnualPlan plan = annualPlanRepository
                .findByYearAndGraduateProgramId(year, graduateProgramId)
                .orElseThrow(AnnualPlanNotFoundException::new);

        AnnualPlanStatus previousStatus = plan.getStatus();
        plan.transitionTo(newStatus);
        AnnualPlan saved = annualPlanRepository.save(plan);
        if (previousStatus == AnnualPlanStatus.TERMINADA
                && newStatus != AnnualPlanStatus.TERMINADA) {
            annualPlanChangePort.markAllOutdatedByYear(year, graduateProgramId);
        }
        return saved;
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }
}

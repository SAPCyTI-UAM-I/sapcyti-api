package mx.uam.sapcyti.planning.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.domain.exception.AnnualPlanNotFoundException;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanStatus;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeStatusUseCase {

    private final AnnualPlanRepositoryPort annualPlanRepository;

    @Transactional
    public AnnualPlan execute(int year, AnnualPlanStatus newStatus) {
        Long graduateProgramId = requireTenant();
        AnnualPlan plan = annualPlanRepository
                .findByYearAndGraduateProgramId(year, graduateProgramId)
                .orElseThrow(AnnualPlanNotFoundException::new);

        plan.transitionTo(newStatus);
        return annualPlanRepository.save(plan);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }
}

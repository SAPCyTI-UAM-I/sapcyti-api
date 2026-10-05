package mx.uam.sapcyti.planning.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAnnualPlansUseCase {

    private final AnnualPlanRepositoryPort annualPlanRepository;

    @Transactional(readOnly = true)
    public List<AnnualPlan> execute() {
        return annualPlanRepository.findAllByGraduateProgramIdOrderByYearDesc(requireTenant());
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }
}

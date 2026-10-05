package mx.uam.sapcyti.trimestral.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotFoundException;
import mx.uam.sapcyti.trimestral.domain.model.PlanWarning;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanStatus;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import mx.uam.sapcyti.trimestral.domain.service.WarningEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeTrimestralPlanStatusUseCase {

    private final TrimestralPlanRepositoryPort planRepository;
    private final TrimestralPlanGenerationSupport generationSupport;
    private final WarningEngine warningEngine;
    private final TrimestralPrerequisiteGuard prerequisiteGuard;

    @Transactional
    public TrimestralPlan execute(Long planId, TrimestralPlanStatus newStatus) {
        Long graduateProgramId = requireTenant();
        TrimestralPlan plan = planRepository
                .findByIdAndGraduateProgramId(planId, graduateProgramId)
                .orElseThrow(TrimestralPlanNotFoundException::new);

        prerequisiteGuard.assertSatisfied(plan);
        plan.transitionTo(newStatus);
        TrimestralPlan saved = planRepository.save(plan);
        List<PlanWarning> warnings =
                warningEngine.evaluate(saved, generationSupport.buildWarningContextForPlan(saved));
        saved.replaceWarnings(warnings);
        return planRepository.save(saved);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }
}

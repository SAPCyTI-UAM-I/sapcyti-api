package mx.uam.sapcyti.trimestral.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotFoundException;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegenerateTrimestralPlanUseCase {

    private final TrimestralPlanRepositoryPort planRepository;
    private final TrimestralPlanGenerationSupport generationSupport;
    private final TrimestralPrerequisiteGuard prerequisiteGuard;

    @Transactional
    public TrimestralPlan execute(Long planId) {
        Long graduateProgramId = requireTenant();
        TrimestralPlan plan = planRepository
                .findByIdAndGraduateProgramId(planId, graduateProgramId)
                .orElseThrow(TrimestralPlanNotFoundException::new);

        prerequisiteGuard.assertSatisfied(plan);
        plan.assertEditable();

        plan.clearContent();
        plan.clearOutdated();
        generationSupport.populateFromSurvey(plan);
        TrimestralPlan saved = planRepository.save(plan);
        generationSupport.refreshWarnings(saved);
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

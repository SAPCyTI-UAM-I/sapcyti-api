package mx.uam.sapcyti.trimestral.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.trimestral.application.service.TrimestralPlanGenerationSupport.BlankStudentView;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotFoundException;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTrimestralPlanUseCase {

    private final TrimestralPlanRepositoryPort planRepository;
    private final TrimestralPlanGenerationSupport generationSupport;
    private final TrimestralPrerequisiteGuard prerequisiteGuard;

    @Transactional(readOnly = true)
    public PlanDetail execute(Long planId) {
        Long graduateProgramId = requireTenant();
        TrimestralPlan plan = planRepository
                .findByIdAndGraduateProgramId(planId, graduateProgramId)
                .orElseThrow(TrimestralPlanNotFoundException::new);
        return new PlanDetail(
                plan,
                generationSupport.deriveBlankStudents(plan),
                prerequisiteGuard.evaluate(plan));
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }

    public record PlanDetail(
            TrimestralPlan plan,
            List<BlankStudentView> blankStudents,
            TrimestralPrerequisiteGuard.Prerequisites prerequisites) {
    }
}

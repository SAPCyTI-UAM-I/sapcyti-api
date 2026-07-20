package mx.uam.sapcyti.trimestral.application.service;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListTrimestralPlansUseCase {

    private final TrimestralPlanRepositoryPort planRepository;
    private final TrimestralPlanGenerationSupport generationSupport;

    @Transactional(readOnly = true)
    public List<PlanSummary> execute() {
        Long graduateProgramId = requireTenant();
        return planRepository.findAllByGraduateProgramId(graduateProgramId).stream()
                .sorted(Comparator.comparing(TrimestralPlan::getTerm, TrimestralPlan.termNewestFirst()))
                .map(plan -> new PlanSummary(
                        plan,
                        plan.getGroups().size(),
                        generationSupport.deriveBlankStudents(plan).size()))
                .toList();
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }

    public record PlanSummary(TrimestralPlan plan, int groupCount, int blankCount) {
    }
}

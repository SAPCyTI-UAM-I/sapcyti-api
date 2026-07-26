package mx.uam.sapcyti.trimestral.infrastructure.adapter.out;

import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanChangePort;
import mx.uam.sapcyti.trimestral.domain.model.OutdatedReason;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AnnualPlanChangeAdapter implements AnnualPlanChangePort {

    private final TrimestralPlanRepositoryPort planRepository;

    @Override
    @Transactional
    public void markOutdatedByChangedUeas(
            int year,
            Long graduateProgramId,
            Map<Character, Set<Long>> changedUeaIdsByTrimester) {
        for (TrimestralPlan plan : planRepository.findAllByGraduateProgramId(graduateProgramId)) {
            Set<Long> changed = changedUeaIdsByTrimester.get(
                    Character.toUpperCase(TrimestralPlan.trimesterLetter(plan.getTerm())));
            if (TrimestralPlan.yearFromTerm(plan.getTerm()) == year
                    && changed != null
                    && !changed.isEmpty()) {
                plan.markOutdated(OutdatedReason.ANNUAL_PLAN_CHANGED);
                planRepository.save(plan);
            }
        }
    }

    @Override
    @Transactional
    public void markAllOutdatedByYear(int year, Long graduateProgramId) {
        for (TrimestralPlan plan : planRepository.findAllByGraduateProgramId(graduateProgramId)) {
            if (TrimestralPlan.yearFromTerm(plan.getTerm()) == year) {
                plan.markOutdated(OutdatedReason.ANNUAL_PLAN_CHANGED);
                planRepository.save(plan);
            }
        }
    }
}

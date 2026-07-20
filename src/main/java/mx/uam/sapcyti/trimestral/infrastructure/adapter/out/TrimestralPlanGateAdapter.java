package mx.uam.sapcyti.trimestral.infrastructure.adapter.out;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.survey.domain.port.out.TrimestralPlanGatePort;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanStatus;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TrimestralPlanGateAdapter implements TrimestralPlanGatePort {

    private final TrimestralPlanRepositoryPort planRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<GateStatus> findStatusByTerm(String term, Long graduateProgramId) {
        return planRepository
                .findByTermAndGraduateProgramId(term, graduateProgramId)
                .map(TrimestralPlan::getStatus)
                .map(status -> status == TrimestralPlanStatus.TERMINADA
                        ? GateStatus.TERMINADA
                        : GateStatus.BORRADOR);
    }

    @Override
    @Transactional
    public void markOutdatedByTerm(String term, Long graduateProgramId) {
        planRepository.findByTermAndGraduateProgramId(term, graduateProgramId).ifPresent(plan -> {
            plan.markOutdated();
            planRepository.save(plan);
        });
    }
}

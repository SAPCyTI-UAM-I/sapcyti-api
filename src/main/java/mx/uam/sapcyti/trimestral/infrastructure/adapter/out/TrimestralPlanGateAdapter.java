package mx.uam.sapcyti.trimestral.infrastructure.adapter.out;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.survey.domain.port.out.TrimestralPlanGatePort;
import mx.uam.sapcyti.trimestral.domain.model.OutdatedReason;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TrimestralPlanGateAdapter implements TrimestralPlanGatePort {

    private final TrimestralPlanRepositoryPort planRepository;

    @Override
    @Transactional
    public void markOutdatedBySurveyReopened(String term, Long graduateProgramId) {
        planRepository.findByTermAndGraduateProgramId(term, graduateProgramId).ifPresent(plan -> {
            plan.markOutdated(OutdatedReason.SURVEY_REOPENED);
            planRepository.save(plan);
        });
    }
}

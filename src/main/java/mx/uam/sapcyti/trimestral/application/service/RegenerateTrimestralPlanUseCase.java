package mx.uam.sapcyti.trimestral.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotClosedException;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotFoundException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.trimestral.domain.exception.AnnualPlanRequiredException;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotFoundException;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegenerateTrimestralPlanUseCase {

    private final TrimestralPlanRepositoryPort planRepository;
    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final AnnualPlanRepositoryPort annualPlanRepository;
    private final TrimestralPlanGenerationSupport generationSupport;

    @Transactional
    public TrimestralPlan execute(Long planId) {
        Long graduateProgramId = requireTenant();
        TrimestralPlan plan = planRepository
                .findByIdAndGraduateProgramId(planId, graduateProgramId)
                .orElseThrow(TrimestralPlanNotFoundException::new);

        plan.assertEditable();

        EnrollmentSurvey survey = surveyRepository
                .findByIdAndGraduateProgramId(plan.getSurveyId(), graduateProgramId)
                .orElseThrow(SurveyNotFoundException::new);
        if (survey.getStatus(Instant.now()) != SurveyStatus.CERRADO) {
            throw new SurveyNotClosedException();
        }

        int year = TrimestralPlan.yearFromTerm(plan.getTerm());
        if (!annualPlanRepository.existsByYearAndGraduateProgramId(year, graduateProgramId)) {
            throw new AnnualPlanRequiredException();
        }

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

package mx.uam.sapcyti.trimestral.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.infrastructure.security.AuthenticatedUserResolver;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotFoundException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanAlreadyExistsException;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenerateTrimestralPlanUseCase {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final TrimestralPlanRepositoryPort planRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TrimestralPlanGenerationSupport generationSupport;
    private final TrimestralPrerequisiteGuard prerequisiteGuard;

    @Transactional
    public TrimestralPlan execute(Long surveyId) {
        Long graduateProgramId = requireTenant();
        EnrollmentSurvey survey = surveyRepository
                .findByIdAndGraduateProgramId(surveyId, graduateProgramId)
                .orElseThrow(SurveyNotFoundException::new);

        prerequisiteGuard.assertSatisfied(survey, graduateProgramId);

        if (planRepository.existsByTermAndGraduateProgramId(survey.getTerm(), graduateProgramId)) {
            throw new TrimestralPlanAlreadyExistsException();
        }

        Long createdBy = authenticatedUserResolver.resolve().getUserId();
        TrimestralPlan plan =
                TrimestralPlan.create(graduateProgramId, survey.getId(), survey.getTerm(), createdBy);
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

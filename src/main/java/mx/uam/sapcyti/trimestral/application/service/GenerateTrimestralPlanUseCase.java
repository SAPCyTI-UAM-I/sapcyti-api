package mx.uam.sapcyti.trimestral.application.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.infrastructure.security.AuthenticatedUserResolver;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotClosedException;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotFoundException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.trimestral.domain.exception.AnnualPlanRequiredException;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanAlreadyExistsException;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenerateTrimestralPlanUseCase {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final AnnualPlanRepositoryPort annualPlanRepository;
    private final TrimestralPlanRepositoryPort planRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TrimestralPlanGenerationSupport generationSupport;

    @Transactional
    public TrimestralPlan execute(Long surveyId) {
        Long graduateProgramId = requireTenant();
        EnrollmentSurvey survey = surveyRepository
                .findByIdAndGraduateProgramId(surveyId, graduateProgramId)
                .orElseThrow(SurveyNotFoundException::new);

        if (survey.getStatus(Instant.now()) != SurveyStatus.CERRADO) {
            throw new SurveyNotClosedException();
        }

        if (planRepository.existsByTermAndGraduateProgramId(survey.getTerm(), graduateProgramId)) {
            throw new TrimestralPlanAlreadyExistsException();
        }

        int year = TrimestralPlan.yearFromTerm(survey.getTerm());
        if (!annualPlanRepository.existsByYearAndGraduateProgramId(year, graduateProgramId)) {
            throw new AnnualPlanRequiredException();
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

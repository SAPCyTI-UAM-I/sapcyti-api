package mx.uam.sapcyti.survey.application.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotFoundException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.survey.domain.service.SuggestedTermCalculator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SurveyDetailFactory {

    private final EnrollmentSurveyRepositoryPort surveyRepository;

    public SurveyDetail toDetail(EnrollmentSurvey survey) {
        long responseCount = surveyRepository.countResponsesBySurveyId(survey.getId());
        return SurveyDetail.builder()
                .survey(survey)
                .responseCount(responseCount)
                .suggestedTerm(resolveSuggestedTerm(survey.getGraduateProgramId()).orElse(null))
                .build();
    }

    public Optional<String> resolveSuggestedTerm(Long graduateProgramId) {
        return surveyRepository.findLatestByGraduateProgramId(graduateProgramId)
                .flatMap(latest -> SuggestedTermCalculator.suggestNext(latest.getTerm()));
    }

    public EnrollmentSurvey requireSurvey(Long surveyId) {
        Long graduateProgramId = requireTenant();
        return surveyRepository.findByIdAndGraduateProgramId(surveyId, graduateProgramId)
                .orElseThrow(SurveyNotFoundException::new);
    }

    static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }
}

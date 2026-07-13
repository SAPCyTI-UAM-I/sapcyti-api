package mx.uam.sapcyti.survey.application.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.infrastructure.security.AuthenticatedUserResolver;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.survey.application.command.CreateSurveyCommand;
import mx.uam.sapcyti.survey.domain.exception.SurveyAlreadyExistsForTermException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.survey.domain.service.SuggestedTermCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSurveyUseCase {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final UeaRepositoryPort ueaRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Transactional
    public SurveyDetail execute(CreateSurveyCommand command) {
        Long graduateProgramId = requireTenant();
        if (surveyRepository.existsByTermAndGraduateProgramId(command.term(), graduateProgramId)) {
            throw new SurveyAlreadyExistsForTermException();
        }

        List<Long> snapshotUeaIds = ueaRepository.findActiveByGraduateProgramId(graduateProgramId).stream()
                .map(UEA::getId)
                .toList();

        Long createdBy = authenticatedUserResolver.resolve().getUserId();
        EnrollmentSurvey survey = EnrollmentSurvey.create(
                graduateProgramId,
                command.term(),
                command.opensAt(),
                command.closesAt(),
                command.introMessage(),
                createdBy,
                snapshotUeaIds);

        EnrollmentSurvey saved = surveyRepository.save(survey);
        String suggestedTerm = SuggestedTermCalculator.suggestNext(command.term()).orElse(null);
        return SurveyDetail.builder()
                .survey(saved)
                .responseCount(0)
                .suggestedTerm(suggestedTerm)
                .build();
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }
}

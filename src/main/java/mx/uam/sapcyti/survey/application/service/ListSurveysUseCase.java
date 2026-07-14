package mx.uam.sapcyti.survey.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListSurveysUseCase {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final SurveyDetailFactory surveyDetailFactory;

    @Transactional(readOnly = true)
    public List<SurveyDetail> execute() {
        Long graduateProgramId = SurveyDetailFactory.requireTenant();
        return surveyRepository.findAllByGraduateProgramIdOrderByCreatedAtDesc(graduateProgramId).stream()
                .map(surveyDetailFactory::toDetail)
                .toList();
    }
}

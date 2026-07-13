package mx.uam.sapcyti.survey.application.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotActiveException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CloseSurveyUseCase {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final SurveyDetailFactory surveyDetailFactory;

    @Transactional
    public SurveyDetail execute(Long surveyId) {
        EnrollmentSurvey survey = surveyDetailFactory.requireSurvey(surveyId);
        if (survey.getStatus(Instant.now()) != SurveyStatus.ACTIVO) {
            throw new SurveyNotActiveException();
        }
        survey.closeManually();
        return surveyDetailFactory.toDetail(surveyRepository.save(survey));
    }
}

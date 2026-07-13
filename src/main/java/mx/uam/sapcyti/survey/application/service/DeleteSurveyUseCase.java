package mx.uam.sapcyti.survey.application.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotDeletableException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteSurveyUseCase {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final SurveyResponseRepositoryPort responseRepository;
    private final SurveyDetailFactory surveyDetailFactory;

    @Transactional
    public void execute(Long surveyId) {
        EnrollmentSurvey survey = surveyDetailFactory.requireSurvey(surveyId);
        long responseCount = responseRepository.countBySurveyId(surveyId);
        if (survey.getStatus(Instant.now()) != SurveyStatus.PROGRAMADO || responseCount > 0) {
            throw new SurveyNotDeletableException();
        }
        surveyRepository.delete(survey);
    }
}

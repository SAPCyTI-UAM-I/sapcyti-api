package mx.uam.sapcyti.survey.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetSurveyUseCase {

    private final SurveyDetailFactory surveyDetailFactory;

    @Transactional(readOnly = true)
    public SurveyDetail execute(Long surveyId) {
        return surveyDetailFactory.toDetail(surveyDetailFactory.requireSurvey(surveyId));
    }
}

package mx.uam.sapcyti.survey.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetSurveyResultsSummaryUseCase {

    private final StudentRepositoryPort studentRepository;
    private final UserRepositoryPort userRepository;
    private final SurveyResponseRepositoryPort responseRepository;
    private final SurveyDetailFactory surveyDetailFactory;

    @Transactional(readOnly = true)
    public SurveyResultsSummary execute(Long surveyId) {
        var survey = surveyDetailFactory.requireSurvey(surveyId);
        Long graduateProgramId = survey.getGraduateProgramId();

        long eligibleCount = 0;
        for (Student student : studentRepository.findByGraduateProgramId(graduateProgramId)) {
            User user = userRepository.findById(student.getUserId()).orElse(null);
            if (user != null && user.isActive()) {
                eligibleCount++;
            }
        }

        long respondedCount = responseRepository.countBySurveyId(surveyId);
        return SurveyResultsSummary.builder()
                .eligibleCount(eligibleCount)
                .respondedCount(respondedCount)
                .pendingCount(Math.max(0, eligibleCount - respondedCount))
                .blankCount(responseRepository.countBlankBySurveyId(surveyId))
                .build();
    }
}

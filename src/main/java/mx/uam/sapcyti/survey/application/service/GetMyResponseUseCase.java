package mx.uam.sapcyti.survey.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.survey.domain.exception.StudentSurveyResponseNotFoundException;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyResponseUseCase {

    private final SurveyResponseRepositoryPort responseRepository;
    private final SurveyDetailFactory surveyDetailFactory;
    private final StudentSurveyContextResolver studentContextResolver;

    @Transactional(readOnly = true)
    public SubmittedResponseView execute(Long surveyId) {
        surveyDetailFactory.requireSurvey(surveyId);
        Student student = studentContextResolver.requireCurrentStudent();
        return responseRepository.findBySurveyIdAndStudentId(surveyId, student.getId())
                .map(SubmittedResponseView::from)
                .orElseThrow(StudentSurveyResponseNotFoundException::new);
    }
}

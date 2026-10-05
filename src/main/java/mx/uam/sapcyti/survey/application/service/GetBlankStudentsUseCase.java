package mx.uam.sapcyti.survey.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-42 — students who answered inscripción en blanco: they count as responded but never
 * show up in the UEA demand rows, so the results screen lists them separately.
 */
@Service
@RequiredArgsConstructor
public class GetBlankStudentsUseCase {

    private final SurveyResponseRepositoryPort responseRepository;
    private final SurveyDetailFactory surveyDetailFactory;

    @Transactional(readOnly = true)
    public List<InterestedStudentView> execute(Long surveyId) {
        var survey = surveyDetailFactory.requireSurvey(surveyId);

        return responseRepository.findBlankStudents(surveyId, survey.getGraduateProgramId()).stream()
                .map(row -> InterestedStudentView.builder()
                        .fullName(row.fullName())
                        .enrollmentId(row.enrollmentId())
                        .academicTerm(row.academicTerm())
                        .build())
                .toList();
    }
}

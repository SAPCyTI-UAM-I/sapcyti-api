package mx.uam.sapcyti.survey.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.exception.UeaNotFoundException;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUeaInterestedStudentsUseCase {

    private final UeaRepositoryPort ueaRepository;
    private final SurveyResponseRepositoryPort responseRepository;
    private final SurveyDetailFactory surveyDetailFactory;

    @Transactional(readOnly = true)
    public List<InterestedStudentView> execute(Long surveyId, Long ueaId) {
        var survey = surveyDetailFactory.requireSurvey(surveyId);
        ueaRepository.findByIdAndGraduateProgramId(ueaId, survey.getGraduateProgramId())
                .orElseThrow(UeaNotFoundException::new);

        return responseRepository.findInterestedStudents(surveyId, ueaId, survey.getGraduateProgramId()).stream()
                .map(row -> InterestedStudentView.builder()
                        .fullName(row.fullName())
                        .enrollmentId(row.enrollmentId())
                        .academicTerm(row.academicTerm())
                        .build())
                .toList();
    }
}

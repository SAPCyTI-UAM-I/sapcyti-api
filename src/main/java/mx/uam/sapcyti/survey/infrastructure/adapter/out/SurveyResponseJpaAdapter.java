package mx.uam.sapcyti.survey.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import mx.uam.sapcyti.survey.infrastructure.adapter.out.repository.SpringDataSurveyResponseRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class SurveyResponseJpaAdapter implements SurveyResponseRepositoryPort {

    private final SpringDataSurveyResponseRepository jpaRepository;

    @Override
    @Transactional
    public StudentSurveyResponse save(StudentSurveyResponse response) {
        return jpaRepository.save(response);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudentSurveyResponse> findBySurveyIdAndStudentId(Long surveyId, Long studentId) {
        return jpaRepository.findBySurveyIdAndStudentId(surveyId, studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySurveyId(Long surveyId) {
        return jpaRepository.countBySurveyId(surveyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UeaDemandAggregate> countResponsesByUeaId(Long surveyId) {
        return jpaRepository.countResponsesGroupedByUeaId(surveyId).stream()
                .map(row -> new UeaDemandAggregate((Long) row[0], (Long) row[1]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterestedStudentRow> findInterestedStudents(
            Long surveyId, Long ueaId, Long graduateProgramId) {
        return jpaRepository.findInterestedStudentsNative(surveyId, ueaId, graduateProgramId).stream()
                .map(row -> new InterestedStudentRow((String) row[0], (String) row[1], (String) row[2]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentSurveyResponse> findAllBySurveyId(Long surveyId) {
        return jpaRepository.findAllBySurveyId(surveyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentSurveyResponse> findAllByStudentIdAndGraduateProgramId(
            Long studentId, Long graduateProgramId) {
        return jpaRepository.findAllByStudentIdAndGraduateProgramId(studentId, graduateProgramId);
    }
}

package mx.uam.sapcyti.survey.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;

public interface SurveyResponseRepositoryPort {

    StudentSurveyResponse save(StudentSurveyResponse response);

    Optional<StudentSurveyResponse> findBySurveyIdAndStudentId(Long surveyId, Long studentId);

    long countBySurveyId(Long surveyId);

    List<UeaDemandAggregate> countResponsesByUeaId(Long surveyId);

    List<InterestedStudentRow> findInterestedStudents(Long surveyId, Long ueaId, Long graduateProgramId);

    record UeaDemandAggregate(Long ueaId, long totalResponses) {}

    record InterestedStudentRow(String fullName, String enrollmentId) {}
}

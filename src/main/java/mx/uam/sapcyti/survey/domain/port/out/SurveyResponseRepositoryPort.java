package mx.uam.sapcyti.survey.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;

public interface SurveyResponseRepositoryPort {

    StudentSurveyResponse save(StudentSurveyResponse response);

    Optional<StudentSurveyResponse> findBySurveyIdAndStudentId(Long surveyId, Long studentId);

    long countBySurveyId(Long surveyId);

    /** Responses with mode BLANK (inscripción en blanco) for the survey (HU-42). */
    long countBlankBySurveyId(Long surveyId);

    List<UeaDemandAggregate> countResponsesByUeaId(Long surveyId);

    List<InterestedStudentRow> findInterestedStudents(Long surveyId, Long ueaId, Long graduateProgramId);

    /** Students who answered inscripción en blanco, matrícula ascending (HU-42). */
    List<InterestedStudentRow> findBlankStudents(Long surveyId, Long graduateProgramId);

    /**
     * Raw responses for trimestral plan generation (SPEC-035). Includes BLANK modes.
     */
    List<StudentSurveyResponse> findAllBySurveyId(Long surveyId);

    /** All responses for a student within a tenant (HU-61 enrollment history). */
    List<StudentSurveyResponse> findAllByStudentIdAndGraduateProgramId(Long studentId, Long graduateProgramId);

    record UeaDemandAggregate(Long ueaId, long totalResponses) {}

    record InterestedStudentRow(String fullName, String enrollmentId, String academicTerm) {}
}

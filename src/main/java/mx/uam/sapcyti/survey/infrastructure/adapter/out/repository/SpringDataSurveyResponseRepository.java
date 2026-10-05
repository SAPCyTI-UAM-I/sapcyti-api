package mx.uam.sapcyti.survey.infrastructure.adapter.out.repository;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSurveyResponseRepository extends JpaRepository<StudentSurveyResponse, Long> {

    Optional<StudentSurveyResponse> findBySurveyIdAndStudentId(Long surveyId, Long studentId);

    List<StudentSurveyResponse> findAllBySurveyId(Long surveyId);

    @Query("""
            SELECT r
            FROM StudentSurveyResponse r, EnrollmentSurvey s
            WHERE r.surveyId = s.id
              AND r.studentId = :studentId
              AND s.graduateProgramId = :graduateProgramId
            """)
    List<StudentSurveyResponse> findAllByStudentIdAndGraduateProgramId(
            @Param("studentId") Long studentId, @Param("graduateProgramId") Long graduateProgramId);

    long countBySurveyId(Long surveyId);

    long countBySurveyIdAndMode(Long surveyId, SurveyResponseMode mode);

    @Query("""
            SELECT ueaId, COUNT(ueaId)
            FROM StudentSurveyResponse r
            JOIN r.ueaIds ueaId
            WHERE r.surveyId = :surveyId
            GROUP BY ueaId
            """)
    List<Object[]> countResponsesGroupedByUeaId(@Param("surveyId") Long surveyId);

    @Query(value = """
            SELECT CONCAT(s.first_name, ' ', s.first_last_name,
                   CASE WHEN s.second_last_name IS NOT NULL AND s.second_last_name <> ''
                        THEN CONCAT(' ', s.second_last_name) ELSE '' END),
                   s.enrollment_id,
                   sr.academic_term
            FROM survey_response_ueas sru
            JOIN survey_responses sr ON sr.id = sru.response_id
            JOIN students s ON s.id = sr.student_id
            WHERE sr.survey_id = :surveyId
              AND sru.uea_id = :ueaId
              AND s.graduate_program_id = :programId
            ORDER BY s.enrollment_id
            """, nativeQuery = true)
    List<Object[]> findInterestedStudentsNative(
            @Param("surveyId") Long surveyId,
            @Param("ueaId") Long ueaId,
            @Param("programId") Long graduateProgramId);

    @Query(value = """
            SELECT CONCAT(s.first_name, ' ', s.first_last_name,
                   CASE WHEN s.second_last_name IS NOT NULL AND s.second_last_name <> ''
                        THEN CONCAT(' ', s.second_last_name) ELSE '' END),
                   s.enrollment_id,
                   sr.academic_term
            FROM survey_responses sr
            JOIN students s ON s.id = sr.student_id
            WHERE sr.survey_id = :surveyId
              AND sr.mode = 'BLANK'
              AND s.graduate_program_id = :programId
            ORDER BY s.enrollment_id
            """, nativeQuery = true)
    List<Object[]> findBlankStudentsNative(
            @Param("surveyId") Long surveyId,
            @Param("programId") Long graduateProgramId);
}

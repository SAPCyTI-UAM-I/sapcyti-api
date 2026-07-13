package mx.uam.sapcyti.survey.infrastructure.adapter.out.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataEnrollmentSurveyRepository extends JpaRepository<EnrollmentSurvey, Long> {

    Optional<EnrollmentSurvey> findByIdAndGraduateProgramId(Long id, Long graduateProgramId);

    List<EnrollmentSurvey> findAllByGraduateProgramIdOrderByCreatedAtDesc(Long graduateProgramId);

    boolean existsByTermAndGraduateProgramId(String term, Long graduateProgramId);

    Optional<EnrollmentSurvey> findFirstByGraduateProgramIdOrderByCreatedAtDesc(Long graduateProgramId);

    @Query("""
            SELECT s FROM EnrollmentSurvey s
            WHERE s.graduateProgramId = :programId
              AND s.closedManually = false
              AND s.opensAt <= :now
              AND s.closesAt > :now
            """)
    Optional<EnrollmentSurvey> findActiveByGraduateProgramId(
            @Param("programId") Long graduateProgramId,
            @Param("now") Instant now);

    @Query("""
            SELECT s.term FROM EnrollmentSurvey s
            WHERE s.graduateProgramId = :programId
              AND :ueaId MEMBER OF s.snapshotUeaIds
              AND s.closedManually = false
              AND s.opensAt <= :now
              AND s.closesAt > :now
            """)
    Optional<String> findActiveSurveyTermIncludingUea(
            @Param("ueaId") Long ueaId,
            @Param("programId") Long graduateProgramId,
            @Param("now") Instant now);
}

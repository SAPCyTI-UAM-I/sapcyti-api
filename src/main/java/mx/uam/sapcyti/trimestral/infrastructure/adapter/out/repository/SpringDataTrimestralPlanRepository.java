package mx.uam.sapcyti.trimestral.infrastructure.adapter.out.repository;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTrimestralPlanRepository extends JpaRepository<TrimestralPlan, Long> {

    @EntityGraph(attributePaths = "groups")
    Optional<TrimestralPlan> findByIdAndGraduateProgramId(Long id, Long graduateProgramId);

    @EntityGraph(attributePaths = "groups")
    Optional<TrimestralPlan> findByTermAndGraduateProgramId(String term, Long graduateProgramId);

    boolean existsByTermAndGraduateProgramId(String term, Long graduateProgramId);

    @EntityGraph(attributePaths = "groups")
    List<TrimestralPlan> findAllByGraduateProgramId(Long graduateProgramId);

    @EntityGraph(attributePaths = "groups")
    @Query("""
            SELECT DISTINCT p
            FROM TrimestralPlan p
            JOIN p.groups g
            JOIN g.students s
            WHERE p.graduateProgramId = :graduateProgramId
              AND s.studentId = :studentId
            """)
    List<TrimestralPlan> findAllByGraduateProgramIdAndStudentId(
            @Param("graduateProgramId") Long graduateProgramId, @Param("studentId") Long studentId);
}

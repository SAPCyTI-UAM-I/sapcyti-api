package mx.uam.sapcyti.planning.infrastructure.adapter.out.repository;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAnnualPlanRepository extends JpaRepository<AnnualPlan, Long> {

    boolean existsByYearAndGraduateProgramId(int year, Long graduateProgramId);

    @EntityGraph(attributePaths = "entries")
    Optional<AnnualPlan> findByYearAndGraduateProgramId(int year, Long graduateProgramId);

    List<AnnualPlan> findAllByGraduateProgramIdOrderByYearDesc(Long graduateProgramId);
}

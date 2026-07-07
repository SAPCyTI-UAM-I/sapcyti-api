package mx.uam.sapcyti.planning.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;

public interface AnnualPlanRepositoryPort {

    AnnualPlan save(AnnualPlan plan);

    boolean existsByYearAndGraduateProgramId(int year, Long graduateProgramId);

    Optional<AnnualPlan> findByYearAndGraduateProgramId(int year, Long graduateProgramId);

    List<AnnualPlan> findAllByGraduateProgramIdOrderByYearDesc(Long graduateProgramId);
}

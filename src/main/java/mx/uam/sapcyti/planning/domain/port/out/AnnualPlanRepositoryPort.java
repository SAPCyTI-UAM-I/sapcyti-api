package mx.uam.sapcyti.planning.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanQuota;

public interface AnnualPlanRepositoryPort {

    AnnualPlan save(AnnualPlan plan);

    boolean existsByYearAndGraduateProgramId(int year, Long graduateProgramId);

    Optional<AnnualPlan> findByYearAndGraduateProgramId(int year, Long graduateProgramId);

    List<AnnualPlan> findAllByGraduateProgramIdOrderByYearDesc(Long graduateProgramId);

    /**
     * Thin quota projection for trimestral planning (SPEC-035). Empty if no annual plan.
     */
    List<AnnualPlanQuota> findQuotas(int year, Long graduateProgramId);
}

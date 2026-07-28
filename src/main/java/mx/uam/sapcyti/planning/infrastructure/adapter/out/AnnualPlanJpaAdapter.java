package mx.uam.sapcyti.planning.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanQuota;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.planning.infrastructure.adapter.out.repository.SpringDataAnnualPlanRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AnnualPlanJpaAdapter implements AnnualPlanRepositoryPort {

    private final SpringDataAnnualPlanRepository jpaRepository;

    @Override
    @Transactional
    public AnnualPlan save(AnnualPlan plan) {
        return jpaRepository.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByYearAndGraduateProgramId(int year, Long graduateProgramId) {
        return jpaRepository.existsByYearAndGraduateProgramId(year, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnnualPlan> findByYearAndGraduateProgramId(int year, Long graduateProgramId) {
        return jpaRepository.findByYearAndGraduateProgramId(year, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnualPlan> findAllByGraduateProgramIdOrderByYearDesc(Long graduateProgramId) {
        return jpaRepository.findAllByGraduateProgramIdOrderByYearDesc(graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnualPlanQuota> findQuotas(int year, Long graduateProgramId) {
        return jpaRepository
                .findByYearAndGraduateProgramId(year, graduateProgramId)
                .map(plan -> plan.getEntries().stream()
                        .map(entry -> new AnnualPlanQuota(
                                entry.getUeaId(),
                                entry.getGruposI(),
                                entry.getCupoI(),
                                entry.getGruposP(),
                                entry.getCupoP(),
                                entry.getGruposO(),
                                entry.getCupoO()))
                        .toList())
                .orElse(List.of());
    }
}

package mx.uam.sapcyti.trimestral.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.out.repository.SpringDataTrimestralPlanRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class TrimestralPlanJpaAdapter implements TrimestralPlanRepositoryPort {

    private final SpringDataTrimestralPlanRepository jpaRepository;

    @Override
    @Transactional
    public TrimestralPlan save(TrimestralPlan plan) {
        TrimestralPlan saved = jpaRepository.save(plan);
        initializeCollections(saved);
        return saved;
    }

    @Override
    @Transactional
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrimestralPlan> findByIdAndGraduateProgramId(Long id, Long graduateProgramId) {
        Optional<TrimestralPlan> plan = jpaRepository.findByIdAndGraduateProgramId(id, graduateProgramId);
        plan.ifPresent(this::initializeCollections);
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrimestralPlan> findByTermAndGraduateProgramId(String term, Long graduateProgramId) {
        Optional<TrimestralPlan> plan = jpaRepository.findByTermAndGraduateProgramId(term, graduateProgramId);
        plan.ifPresent(this::initializeCollections);
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTermAndGraduateProgramId(String term, Long graduateProgramId) {
        return jpaRepository.existsByTermAndGraduateProgramId(term, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrimestralPlan> findAllByGraduateProgramId(Long graduateProgramId) {
        List<TrimestralPlan> plans = jpaRepository.findAllByGraduateProgramId(graduateProgramId);
        plans.forEach(this::initializeCollections);
        return plans;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrimestralPlan> findAllByGraduateProgramIdAndStudentId(
            Long graduateProgramId, Long studentId) {
        List<TrimestralPlan> plans =
                jpaRepository.findAllByGraduateProgramIdAndStudentId(graduateProgramId, studentId);
        plans.forEach(this::initializeCollections);
        return plans;
    }

    private void initializeCollections(TrimestralPlan plan) {
        plan.getWarnings().size();
        plan.getUnassignedDemand().size();
        plan.getOutdatedReasons().size();
        for (TrimestralPlanGroup group : plan.getGroups()) {
            group.getStudents().size();
            group.getProfessors().size();
        }
    }
}

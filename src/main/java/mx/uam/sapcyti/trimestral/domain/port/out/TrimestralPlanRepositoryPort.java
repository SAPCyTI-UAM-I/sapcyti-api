package mx.uam.sapcyti.trimestral.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;

public interface TrimestralPlanRepositoryPort {

    TrimestralPlan save(TrimestralPlan plan);

    Optional<TrimestralPlan> findByIdAndGraduateProgramId(Long id, Long graduateProgramId);

    Optional<TrimestralPlan> findByTermAndGraduateProgramId(String term, Long graduateProgramId);

    boolean existsByTermAndGraduateProgramId(String term, Long graduateProgramId);

    List<TrimestralPlan> findAllByGraduateProgramId(Long graduateProgramId);

    List<TrimestralPlan> findAllByGraduateProgramIdAndStudentId(Long graduateProgramId, Long studentId);
}

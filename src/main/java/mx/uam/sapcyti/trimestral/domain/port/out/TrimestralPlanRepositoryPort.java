package mx.uam.sapcyti.trimestral.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;

public interface TrimestralPlanRepositoryPort {

    TrimestralPlan save(TrimestralPlan plan);

    /** Baja lo pendiente a la base: vaciar y repoblar en un solo guardado invierte el orden. */
    void flush();

    Optional<TrimestralPlan> findByIdAndGraduateProgramId(Long id, Long graduateProgramId);

    Optional<TrimestralPlan> findByTermAndGraduateProgramId(String term, Long graduateProgramId);

    boolean existsByTermAndGraduateProgramId(String term, Long graduateProgramId);

    List<TrimestralPlan> findAllByGraduateProgramId(Long graduateProgramId);

    List<TrimestralPlan> findAllByGraduateProgramIdAndStudentId(Long graduateProgramId, Long studentId);
}

package mx.uam.sapcyti.academic.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.Professor;

public interface ProfessorRepositoryPort {

    Professor save(Professor professor);

    boolean existsByIdAndGraduateProgramId(Long id, Long graduateProgramId);

    List<Professor> findByGraduateProgramId(Long graduateProgramId);

    List<Professor> findInternosByEmployeeNumberAndGraduateProgramId(
            Long graduateProgramId, String employeeNumber);

    Optional<Professor> findById(Long id);

    Optional<Professor> findByIdAndGraduateProgramId(Long id, Long graduateProgramId);
}

package mx.uam.sapcyti.academic.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.Professor;

public interface ProfessorRepositoryPort {

    Professor save(Professor professor);

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByIdAndGraduateProgramId(Long id, Long graduateProgramId);

    List<Professor> findByGraduateProgramId(Long graduateProgramId);

    Optional<Professor> findById(Long id);
}

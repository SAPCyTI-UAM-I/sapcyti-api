package mx.uam.sapcyti.academic.infrastructure.adapter.out.repository;

import java.util.List;
import mx.uam.sapcyti.academic.domain.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProfessorRepository extends JpaRepository<Professor, Long> {

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByIdAndGraduateProgramId(Long id, Long graduateProgramId);

    List<Professor> findByGraduateProgramId(Long graduateProgramId);
}

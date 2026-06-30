package mx.uam.sapcyti.academic.infrastructure.adapter.out.repository;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProfessorRepository extends JpaRepository<Professor, Long> {

    boolean existsByIdAndGraduateProgramId(Long id, Long graduateProgramId);

    List<Professor> findByGraduateProgramId(Long graduateProgramId);

    List<Professor> findByGraduateProgramIdAndProfessorTypeAndEmployeeNumber(
            Long graduateProgramId, ProfessorType professorType, String employeeNumber);

    Optional<Professor> findByIdAndGraduateProgramId(Long id, Long graduateProgramId);
}

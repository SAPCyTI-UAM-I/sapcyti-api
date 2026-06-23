package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataProfessorRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ProfessorJpaAdapter implements ProfessorRepositoryPort {

    private final SpringDataProfessorRepository jpaRepository;

    @Override
    @Transactional
    public Professor save(Professor professor) {
        return jpaRepository.save(professor);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmployeeNumber(String employeeNumber) {
        return jpaRepository.existsByEmployeeNumber(employeeNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdAndGraduateProgramId(Long id, Long graduateProgramId) {
        return jpaRepository.existsByIdAndGraduateProgramId(id, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Professor> findByGraduateProgramId(Long graduateProgramId) {
        return jpaRepository.findByGraduateProgramId(graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Professor> findById(Long id) {
        return jpaRepository.findById(id);
    }
}

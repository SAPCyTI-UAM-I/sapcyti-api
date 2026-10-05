package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;
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
    public List<Professor> findInternosByEmployeeNumberAndGraduateProgramId(
            Long graduateProgramId, String employeeNumber) {
        return jpaRepository.findByGraduateProgramIdAndProfessorTypeAndEmployeeNumber(
                graduateProgramId, ProfessorType.INTERNO, employeeNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Professor> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Professor> findByIdAndGraduateProgramId(Long id, Long graduateProgramId) {
        return jpaRepository.findByIdAndGraduateProgramId(id, graduateProgramId);
    }
}

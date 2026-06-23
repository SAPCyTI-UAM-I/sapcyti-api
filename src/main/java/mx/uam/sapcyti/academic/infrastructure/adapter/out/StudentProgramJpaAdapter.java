package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataStudentProgramRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class StudentProgramJpaAdapter implements StudentProgramRepositoryPort {

    private final SpringDataStudentProgramRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StudentProgram> findByStudentIdAndGraduateProgramId(
            Long studentId, Long graduateProgramId) {
        return jpaRepository.findByStudentIdAndGraduateProgramIdWithAdvisors(studentId, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudentProgram> findByIdAndStudentIdAndGraduateProgramId(
            Long id, Long studentId, Long graduateProgramId) {
        return jpaRepository.findByIdAndStudentIdAndGraduateProgramIdWithAdvisors(
                id, studentId, graduateProgramId);
    }

    @Override
    @Transactional
    public StudentProgram save(StudentProgram program) {
        return jpaRepository.save(program);
    }
}

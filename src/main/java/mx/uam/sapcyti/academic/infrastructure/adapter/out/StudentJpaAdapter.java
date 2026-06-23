package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataStudentRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class StudentJpaAdapter implements StudentRepositoryPort {

    private final SpringDataStudentRepository jpaRepository;

    @Override
    @Transactional
    public Student save(Student student) {
        return jpaRepository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEnrollmentId(String enrollmentId) {
        return jpaRepository.existsByEnrollmentId(enrollmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findByGraduateProgramId(Long graduateProgramId) {
        return jpaRepository.findByGraduateProgramId(graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Student> findById(Long id) {
        return jpaRepository.findById(id);
    }
}

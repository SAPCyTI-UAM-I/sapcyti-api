package mx.uam.sapcyti.academic.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.Student;

public interface StudentRepositoryPort {

    Student save(Student student);

    boolean existsByEnrollmentId(String enrollmentId);

    List<Student> findByGraduateProgramId(Long graduateProgramId);

    Optional<Student> findById(Long id);

    Optional<Student> findByUserIdAndGraduateProgramId(Long userId, Long graduateProgramId);
}

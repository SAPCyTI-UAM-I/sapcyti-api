package mx.uam.sapcyti.academic.infrastructure.adapter.out.repository;

import java.util.List;
import mx.uam.sapcyti.academic.domain.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEnrollmentId(String enrollmentId);

    List<Student> findByGraduateProgramId(Long graduateProgramId);
}

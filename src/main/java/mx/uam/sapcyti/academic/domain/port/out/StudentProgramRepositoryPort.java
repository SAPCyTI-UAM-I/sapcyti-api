package mx.uam.sapcyti.academic.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;

public interface StudentProgramRepositoryPort {

    List<StudentProgram> findByStudentIdAndGraduateProgramId(Long studentId, Long graduateProgramId);

    Optional<StudentProgram> findByIdAndStudentIdAndGraduateProgramId(
            Long id, Long studentId, Long graduateProgramId);

    StudentProgram save(StudentProgram program);
}

package mx.uam.sapcyti.academic.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.service.GetStudentProgramUseCase.StudentProgramDetail;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetStudentDetailUseCase {

    private final StudentRepositoryPort studentRepository;
    private final StudentProgramRepositoryPort studentProgramRepository;
    private final UserRepositoryPort userRepository;
    private final GetStudentProgramUseCase getStudentProgramUseCase;

    @Transactional(readOnly = true)
    public StudentDetail execute(Long id) {
        Long graduateProgramId = requireTenant();

        Student student = studentRepository.findById(id)
                .orElseThrow(StudentNotFoundException::new);

        if (!graduateProgramId.equals(student.getGraduateProgramId())) {
            throw new StudentNotFoundException();
        }

        User user = userRepository.findById(student.getUserId()).orElse(null);
        ListStudentsUseCase.StudentListItem studentItem = ListStudentsUseCase.toListItem(student, user);

        List<StudentProgram> programs =
                studentProgramRepository.findByStudentIdAndGraduateProgramId(id, graduateProgramId);
        StudentProgramDetail programDetail = programs.isEmpty()
                ? null
                : getStudentProgramUseCase.execute(id, programs.getFirst().getId());

        return new StudentDetail(studentItem, programDetail);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }

    public record StudentDetail(
            ListStudentsUseCase.StudentListItem student,
            StudentProgramDetail program) {
    }
}

package mx.uam.sapcyti.academic.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetStudentUseCase {

    private final StudentRepositoryPort studentRepository;
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public ListStudentsUseCase.StudentListItem execute(Long id) {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }

        Student student = studentRepository.findById(id)
                .orElseThrow(StudentNotFoundException::new);

        if (!graduateProgramId.equals(student.getGraduateProgramId())) {
            throw new StudentNotFoundException();
        }

        User user = userRepository.findById(student.getUserId()).orElse(null);
        return ListStudentsUseCase.toListItem(student, user);
    }
}

package mx.uam.sapcyti.academic.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.exception.ProfessorAlreadyInactiveException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorHasActiveAssignmentsException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateProfessorUseCase {

    private final ProfessorRepositoryPort professorRepository;
    private final StudentProgramRepositoryPort studentProgramRepository;
    private final UserRepositoryPort userRepository;

    @Transactional
    public ListProfessorsUseCase.ProfessorListItem execute(Long professorId) {
        Long graduateProgramId = requireTenant();

        Professor professor = professorRepository.findByIdAndGraduateProgramId(professorId, graduateProgramId)
                .orElseThrow(ProfessorNotFoundException::new);

        User user = userRepository.findById(professor.getUserId())
                .orElseThrow(ProfessorNotFoundException::new);

        if (!user.isActive()) {
            throw new ProfessorAlreadyInactiveException();
        }

        if (studentProgramRepository.hasActiveAssignmentAsTutorOrAdvisor(professorId)) {
            throw new ProfessorHasActiveAssignmentsException();
        }

        user.setActive(false);
        userRepository.save(user);

        return ListProfessorsUseCase.toListItem(professor, user);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }
}

package mx.uam.sapcyti.academic.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEmployeeNumberException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorAlreadyActiveException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestoreProfessorUseCase {

    private final ProfessorRepositoryPort professorRepository;
    private final UserRepositoryPort userRepository;

    @Transactional
    public ListProfessorsUseCase.ProfessorListItem execute(Long professorId) {
        Long graduateProgramId = requireTenant();

        Professor professor = professorRepository.findByIdAndGraduateProgramId(professorId, graduateProgramId)
                .orElseThrow(ProfessorNotFoundException::new);

        User user = userRepository.findById(professor.getUserId())
                .orElseThrow(ProfessorNotFoundException::new);

        if (user.isActive()) {
            throw new ProfessorAlreadyActiveException();
        }

        assertNoActiveNempCollision(graduateProgramId, professor);

        user.setActive(true);
        userRepository.save(user);

        return ListProfessorsUseCase.toListItem(professor, user);
    }

    private void assertNoActiveNempCollision(Long graduateProgramId, Professor professor) {
        if (professor.getProfessorType() != ProfessorType.INTERNO
                || professor.getEmployeeNumber() == null) {
            return;
        }
        for (Professor candidate : professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(
                graduateProgramId, professor.getEmployeeNumber())) {
            if (professor.getId().equals(candidate.getId())) {
                continue;
            }
            User linkedUser = userRepository.findById(candidate.getUserId()).orElse(null);
            if (linkedUser != null && linkedUser.isActive()) {
                throw new DuplicateEmployeeNumberException();
            }
        }
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }
}

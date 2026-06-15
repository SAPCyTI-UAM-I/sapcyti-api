package mx.uam.sapcyti.academic.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProfessorUseCase {

    private final ProfessorRepositoryPort professorRepository;
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public ListProfessorsUseCase.ProfessorListItem execute(Long id) {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }

        Professor professor = professorRepository.findById(id)
                .orElseThrow(ProfessorNotFoundException::new);

        if (!graduateProgramId.equals(professor.getGraduateProgramId())) {
            throw new ProfessorNotFoundException();
        }

        String email = userRepository.findById(professor.getUserId())
                .map(User::getEmail)
                .orElse(null);
        return ListProfessorsUseCase.toListItem(professor, email);
    }
}

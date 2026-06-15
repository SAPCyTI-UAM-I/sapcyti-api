package mx.uam.sapcyti.academic.application.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
public class ListProfessorsUseCase {

    private final ProfessorRepositoryPort professorRepository;
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public List<ProfessorListItem> execute() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }

        List<ProfessorListItem> items = new ArrayList<>();
        for (Professor professor : professorRepository.findByGraduateProgramId(graduateProgramId)) {
            String email = userRepository.findById(professor.getUserId())
                    .map(User::getEmail)
                    .orElse(null);
            items.add(toListItem(professor, email));
        }
        return items;
    }

    private static ProfessorListItem toListItem(Professor professor, String email) {
        return ProfessorListItem.builder()
                .id(professor.getId())
                .employeeNumber(professor.getEmployeeNumber())
                .email(email)
                .firstName(professor.getPersonalData().getFirstName())
                .firstLastName(professor.getPersonalData().getFirstLastName())
                .secondLastName(professor.getPersonalData().getSecondLastName())
                .graduateProgramId(professor.getGraduateProgramId())
                .build();
    }

    @lombok.Value
    @lombok.Builder
    public static class ProfessorListItem {
        Long id;
        String employeeNumber;
        String email;
        String firstName;
        String firstLastName;
        String secondLastName;
        Long graduateProgramId;
    }
}

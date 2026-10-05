package mx.uam.sapcyti.academic.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.shared.web.PageSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListProfessorsUseCase {

    private final ProfessorRepositoryPort professorRepository;
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public Page<ProfessorListItem> execute(ProfessorListQuery query) {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }

        List<ProfessorListItem> items = new ArrayList<>();
        for (Professor professor : professorRepository.findByGraduateProgramId(graduateProgramId)) {
            User user = userRepository.findById(professor.getUserId()).orElse(null);
            items.add(toListItem(professor, user));
        }

        List<ProfessorListItem> filtered = items.stream()
                .filter(item -> matchesSearch(item, query.search()))
                .filter(item -> query.active() == null || item.isActive() == query.active())
                .toList();

        return PageSupport.paginate(filtered, query.pageable());
    }

    static ProfessorListItem toListItem(Professor professor, User user) {
        return ProfessorListItem.builder()
                .id(professor.getId())
                .professorType(professor.getProfessorType())
                .employeeNumber(professor.getEmployeeNumber())
                .email(user != null ? user.getEmail() : null)
                .firstName(professor.getPersonalData().getFirstName())
                .firstLastName(professor.getPersonalData().getFirstLastName())
                .secondLastName(professor.getPersonalData().getSecondLastName())
                .phone(professor.getPersonalData().getPhone())
                .phoneExtension(professor.getPersonalData().getPhoneExtension())
                .commissionMember(professor.getProfessorInformation().isCommissionMember())
                .nextSabbaticalStart(professor.getProfessorInformation().getNextSabbaticalStart())
                .nextSabbaticalEnd(professor.getProfessorInformation().getNextSabbaticalEnd())
                .graduateProgramId(professor.getGraduateProgramId())
                .userId(professor.getUserId())
                .active(user != null && user.isActive())
                .build();
    }

    private static boolean matchesSearch(ProfessorListItem item, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(item.getFirstName(), needle)
                || containsIgnoreCase(item.getFirstLastName(), needle)
                || containsIgnoreCase(item.getSecondLastName(), needle)
                || containsIgnoreCase(item.getEmail(), needle)
                || containsIgnoreCase(item.getEmployeeNumber(), needle);
    }

    private static boolean containsIgnoreCase(String value, String lowerNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }

    /**
     * Filter and pagination criteria for the professor catalog listing.
     */
    public record ProfessorListQuery(String search, Boolean active, Pageable pageable) {
    }

    @Value
    @Builder
    public static class ProfessorListItem {
        Long id;
        ProfessorType professorType;
        String employeeNumber;
        String email;
        String firstName;
        String firstLastName;
        String secondLastName;
        String phone;
        String phoneExtension;
        boolean commissionMember;
        LocalDate nextSabbaticalStart;
        LocalDate nextSabbaticalEnd;
        Long graduateProgramId;
        Long userId;
        boolean active;
    }
}

package mx.uam.sapcyti.academic.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
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
public class ListStudentsUseCase {

    private final StudentRepositoryPort studentRepository;
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public Page<StudentListItem> execute(StudentListQuery query) {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }

        List<StudentListItem> items = new ArrayList<>();
        for (Student student : studentRepository.findByGraduateProgramId(graduateProgramId)) {
            User user = userRepository.findById(student.getUserId()).orElse(null);
            items.add(toListItem(student, user));
        }

        List<StudentListItem> filtered = items.stream()
                .filter(item -> matchesSearch(item, query.search()))
                .filter(item -> query.programType() == null
                        || item.getProgramType() == query.programType())
                .filter(item -> query.active() == null || item.isActive() == query.active())
                .toList();

        return PageSupport.paginate(filtered, query.pageable());
    }

    static StudentListItem toListItem(Student student, User user) {
        return StudentListItem.builder()
                .id(student.getId())
                .enrollmentId(student.getEnrollmentId())
                .email(user != null ? user.getEmail() : null)
                .firstName(student.getPersonalData().getFirstName())
                .firstLastName(student.getPersonalData().getFirstLastName())
                .secondLastName(student.getPersonalData().getSecondLastName())
                .nationality(student.getPersonalData().getNationality())
                .birthDate(student.getPersonalData().getBirthDate())
                .phone(student.getPersonalData().getPhone())
                .phoneExtension(student.getPersonalData().getPhoneExtension())
                .undergraduateDegree(student.getAcademicInformation().getUndergraduateDegree())
                .lastDegreeObtained(student.getAcademicInformation().getLastDegreeObtained())
                .programType(student.getAcademicInformation().getProgramType())
                .admissionDate(student.getAcademicInformation().getAdmissionDate())
                .admissionTerm(student.getAcademicInformation().getAdmissionTerm())
                .advisorId(student.getAdvisorId())
                .graduateProgramId(student.getGraduateProgramId())
                .userId(student.getUserId())
                .active(user != null && user.isActive())
                .build();
    }

    private static boolean matchesSearch(StudentListItem item, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(item.getFirstName(), needle)
                || containsIgnoreCase(item.getFirstLastName(), needle)
                || containsIgnoreCase(item.getSecondLastName(), needle)
                || containsIgnoreCase(item.getEmail(), needle)
                || containsIgnoreCase(item.getEnrollmentId(), needle);
    }

    private static boolean containsIgnoreCase(String value, String lowerNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }

    /**
     * Filter and pagination criteria for the student catalog listing.
     */
    public record StudentListQuery(
            String search, ProgramType programType, Boolean active, Pageable pageable) {
    }

    @Value
    @Builder
    public static class StudentListItem {
        Long id;
        String enrollmentId;
        String email;
        String firstName;
        String firstLastName;
        String secondLastName;
        String nationality;
        LocalDate birthDate;
        String phone;
        String phoneExtension;
        String undergraduateDegree;
        DegreeLevel lastDegreeObtained;
        ProgramType programType;
        LocalDate admissionDate;
        String admissionTerm;
        Long advisorId;
        Long graduateProgramId;
        Long userId;
        boolean active;
    }
}

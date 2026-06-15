package mx.uam.sapcyti.academic.application.service;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListStudentsUseCase {

    private final StudentRepositoryPort studentRepository;
    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public List<StudentListItem> execute() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }

        List<StudentListItem> items = new ArrayList<>();
        for (Student student : studentRepository.findByGraduateProgramId(graduateProgramId)) {
            String email = userRepository.findById(student.getUserId())
                    .map(User::getEmail)
                    .orElse(null);
            items.add(toListItem(student, email));
        }
        return items;
    }

    static StudentListItem toListItem(Student student, String email) {
        return StudentListItem.builder()
                .id(student.getId())
                .enrollmentId(student.getEnrollmentId())
                .email(email)
                .firstName(student.getPersonalData().getFirstName())
                .firstLastName(student.getPersonalData().getFirstLastName())
                .secondLastName(student.getPersonalData().getSecondLastName())
                .nationality(student.getPersonalData().getNationality())
                .undergraduateDegree(student.getAcademicInformation().getUndergraduateDegree())
                .programType(student.getAcademicInformation().getProgramType())
                .admissionDate(student.getAcademicInformation().getAdmissionDate())
                .advisorId(student.getAdvisorId())
                .graduateProgramId(student.getGraduateProgramId())
                .build();
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
        String undergraduateDegree;
        ProgramType programType;
        LocalDate admissionDate;
        Long advisorId;
        Long graduateProgramId;
    }
}

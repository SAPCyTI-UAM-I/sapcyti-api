package mx.uam.sapcyti.academic.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.command.UpdateStudentCommand;
import mx.uam.sapcyti.academic.domain.exception.DuplicateStudentEmailException;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.model.AcademicInformation;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
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
public class UpdateStudentUseCase {

    private final StudentRepositoryPort studentRepository;
    private final UserRepositoryPort userRepository;

    @Transactional
    public ListStudentsUseCase.StudentListItem execute(UpdateStudentCommand command) {
        Long graduateProgramId = requireTenant();

        Student student = studentRepository.findById(command.studentId())
                .orElseThrow(StudentNotFoundException::new);

        if (!graduateProgramId.equals(student.getGraduateProgramId())) {
            throw new StudentNotFoundException();
        }

        User user = userRepository.findById(student.getUserId())
                .orElseThrow(StudentNotFoundException::new);

        String normalizedEmail = command.email().trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new DuplicateStudentEmailException();
                });

        user.setEmail(normalizedEmail);
        user.setActive(command.active());
        userRepository.save(user);

        PersonalData personalData = new PersonalData(
                command.firstName().trim(),
                command.firstLastName().trim(),
                blankToNull(command.secondLastName()),
                command.nationality().trim(),
                command.birthDate(),
                command.phone().trim(),
                blankToNull(command.phoneExtension()));

        AcademicInformation academicInformation = new AcademicInformation(
                command.undergraduateDegree().trim(),
                command.lastDegreeObtained(),
                command.programType(),
                command.admissionDate());

        student.updateProfile(personalData, academicInformation);
        student = studentRepository.save(student);

        return ListStudentsUseCase.toListItem(student, user);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

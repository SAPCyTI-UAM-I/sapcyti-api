package mx.uam.sapcyti.academic.application.service;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.academic.application.command.RegisterStudentCommand;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEnrollmentIdException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateStudentEmailException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.model.AcademicInformation;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.academic.domain.service.PasswordGenerationService;
import mx.uam.sapcyti.academic.domain.service.ResearchCatalogValidator;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.PasswordEncoderPort;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterStudentUseCase {

    private final GraduateProgramRepositoryPort programRepository;
    private final UserRepositoryPort userRepository;
    private final StudentRepositoryPort studentRepository;
    private final StudentProgramRepositoryPort studentProgramRepository;
    private final ProfessorRepositoryPort professorRepository;
    private final PasswordGenerationService passwordGenerationService;
    private final PasswordEncoderPort passwordEncoder;
    private final ResearchCatalogValidator researchCatalogValidator;

    @Transactional
    public RegisterStudentResult execute(RegisterStudentCommand command) {
        assertTenant(command.graduateProgramId());
        assertProgramExists(command.graduateProgramId());

        List<Long> advisorIds = resolveAdvisorIds(command);
        StudentProgram.validateUniqueAdvisorIds(advisorIds);
        assertProfessorExists(command.tutorId(), command.graduateProgramId());
        for (Long advisorId : advisorIds) {
            assertProfessorExists(advisorId, command.graduateProgramId());
        }
        researchCatalogValidator.validate(command.lineOfKnowledge(), command.researchArea());

        Long legacyAdvisorId = advisorIds.isEmpty() ? command.advisorId() : advisorIds.getFirst();
        assertAdvisorExists(legacyAdvisorId, command.graduateProgramId());

        String normalizedEmail = command.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateStudentEmailException();
        }
        if (studentRepository.existsByEnrollmentId(command.enrollmentId().trim())) {
            throw new DuplicateEnrollmentIdException();
        }

        String plaintextPassword = passwordGenerationService.generatePassword();
        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(plaintextPassword),
                RoleType.STUDENT,
                command.graduateProgramId());
        user = userRepository.save(user);

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
                command.lastDegreeObtained().trim(),
                command.programType(),
                command.admissionDate());

        Student student = new Student(
                command.enrollmentId().trim(),
                user.getId(),
                command.graduateProgramId(),
                legacyAdvisorId,
                personalData,
                academicInformation);
        student = studentRepository.save(student);

        StudentProgram program = StudentProgram.forRegistration(
                student.getId(),
                student.getGraduateProgramId(),
                student.getEnrollmentId(),
                academicInformation.getProgramType(),
                academicInformation.getAdmissionDate(),
                null);
        program.updateMetadata(
                academicInformation.getAdmissionDate(),
                null,
                blankToNull(command.lineOfKnowledge()),
                blankToNull(command.researchArea()),
                mx.uam.sapcyti.academic.domain.model.ProgramStatus.ACTIVO,
                null,
                command.tutorId());
        program.replaceAdvisors(advisorIds);
        studentProgramRepository.save(program);

        return RegisterStudentResult.builder()
                .id(student.getId())
                .userId(user.getId())
                .enrollmentId(student.getEnrollmentId())
                .email(normalizedEmail)
                .firstName(personalData.getFirstName())
                .firstLastName(personalData.getFirstLastName())
                .secondLastName(personalData.getSecondLastName())
                .nationality(personalData.getNationality())
                .birthDate(personalData.getBirthDate())
                .phone(personalData.getPhone())
                .phoneExtension(personalData.getPhoneExtension())
                .undergraduateDegree(academicInformation.getUndergraduateDegree())
                .lastDegreeObtained(academicInformation.getLastDegreeObtained())
                .programType(academicInformation.getProgramType())
                .admissionDate(academicInformation.getAdmissionDate())
                .advisorId(legacyAdvisorId)
                .lineOfKnowledge(blankToNull(command.lineOfKnowledge()))
                .researchArea(blankToNull(command.researchArea()))
                .tutorId(command.tutorId())
                .advisorIds(advisorIds.isEmpty() ? null : advisorIds)
                .graduateProgramId(student.getGraduateProgramId())
                .generatedPassword(plaintextPassword)
                .build();
    }

    private void assertTenant(Long graduateProgramId) {
        Long tenantId = TenantContext.get();
        if (tenantId == null || !tenantId.equals(graduateProgramId)) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISMATCH_MESSAGE);
        }
    }

    private void assertProgramExists(Long graduateProgramId) {
        if (programRepository.findById(graduateProgramId).isEmpty()) {
            throw new GraduateProgramNotFoundException(graduateProgramId);
        }
    }

    private static List<Long> resolveAdvisorIds(RegisterStudentCommand command) {
        if (command.advisorIds() != null && !command.advisorIds().isEmpty()) {
            return command.advisorIds();
        }
        if (command.advisorId() != null) {
            return List.of(command.advisorId());
        }
        return List.of();
    }

    private void assertProfessorExists(Long professorId, Long graduateProgramId) {
        if (professorId == null) {
            return;
        }
        if (!professorRepository.existsByIdAndGraduateProgramId(professorId, graduateProgramId)) {
            throw new ProfessorNotFoundException();
        }
    }

    private void assertAdvisorExists(Long advisorId, Long graduateProgramId) {
        if (advisorId == null) {
            return;
        }
        if (!professorRepository.existsByIdAndGraduateProgramId(advisorId, graduateProgramId)) {
            throw new ProfessorNotFoundException();
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Value
    @Builder
    public static class RegisterStudentResult {
        Long id;
        Long userId;
        String enrollmentId;
        String email;
        String firstName;
        String firstLastName;
        String secondLastName;
        String nationality;
        java.time.LocalDate birthDate;
        String phone;
        String phoneExtension;
        String undergraduateDegree;
        String lastDegreeObtained;
        mx.uam.sapcyti.academic.domain.model.ProgramType programType;
        java.time.LocalDate admissionDate;
        Long advisorId;
        Long tutorId;
        String lineOfKnowledge;
        String researchArea;
        java.util.List<Long> advisorIds;
        Long graduateProgramId;
        String generatedPassword;
    }
}

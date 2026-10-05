package mx.uam.sapcyti.academic.application.service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.academic.application.command.RegisterProfessorCommand;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEmployeeNumberException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateProfessorEmailException;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorInformation;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.service.PasswordGenerationService;
import mx.uam.sapcyti.academic.domain.service.ProfessorTypeRules;
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
public class RegisterProfessorUseCase {

    private final GraduateProgramRepositoryPort programRepository;
    private final UserRepositoryPort userRepository;
    private final ProfessorRepositoryPort professorRepository;
    private final PasswordGenerationService passwordGenerationService;
    private final PasswordEncoderPort passwordEncoder;

    @Transactional
    public RegisterProfessorResult execute(RegisterProfessorCommand command) {
        assertTenant(command.graduateProgramId());
        assertProgramExists(command.graduateProgramId());
        assertValidSabbaticalPeriod(command.nextSabbaticalStart(), command.nextSabbaticalEnd());

        String normalizedEmployeeNumber = ProfessorTypeRules.normalizeEmployeeNumber(
                command.professorType(), command.employeeNumber());

        String normalizedEmail = command.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateProfessorEmailException();
        }
        assertUniqueEmployeeNumber(command.graduateProgramId(), normalizedEmployeeNumber, null);

        String plaintextPassword = passwordGenerationService.generatePassword();
        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(plaintextPassword),
                RoleType.PROFESSOR,
                command.graduateProgramId());
        user = userRepository.save(user);

        PersonalData personalData = new PersonalData(
                command.firstName().trim(),
                command.firstLastName().trim(),
                blankToNull(command.secondLastName()),
                null,
                null,
                command.phone().trim(),
                blankToNull(command.phoneExtension()));

        ProfessorInformation professorInformation = new ProfessorInformation(
                command.commissionMember(),
                command.nextSabbaticalStart(),
                command.nextSabbaticalEnd());

        Professor professor = new Professor(
                command.professorType(),
                normalizedEmployeeNumber,
                user.getId(),
                command.graduateProgramId(),
                personalData,
                professorInformation);
        professor = professorRepository.save(professor);

        return RegisterProfessorResult.builder()
                .id(professor.getId())
                .userId(user.getId())
                .professorType(professor.getProfessorType())
                .employeeNumber(professor.getEmployeeNumber())
                .email(normalizedEmail)
                .firstName(personalData.getFirstName())
                .firstLastName(personalData.getFirstLastName())
                .secondLastName(personalData.getSecondLastName())
                .phone(personalData.getPhone())
                .phoneExtension(personalData.getPhoneExtension())
                .commissionMember(professorInformation.isCommissionMember())
                .nextSabbaticalStart(professorInformation.getNextSabbaticalStart())
                .nextSabbaticalEnd(professorInformation.getNextSabbaticalEnd())
                .graduateProgramId(professor.getGraduateProgramId())
                .generatedPassword(plaintextPassword)
                .build();
    }

    private void assertUniqueEmployeeNumber(
            Long graduateProgramId, String employeeNumber, Long excludeProfessorId) {
        if (employeeNumber == null) {
            return;
        }
        for (Professor candidate : professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(
                graduateProgramId, employeeNumber)) {
            if (excludeProfessorId != null && excludeProfessorId.equals(candidate.getId())) {
                continue;
            }
            User linkedUser = userRepository.findById(candidate.getUserId()).orElse(null);
            if (linkedUser != null && linkedUser.isActive()) {
                throw new DuplicateEmployeeNumberException();
            }
        }
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

    private static void assertValidSabbaticalPeriod(
            java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("Sabbatical end date must be on or after start date");
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
    public static class RegisterProfessorResult {
        Long id;
        Long userId;
        mx.uam.sapcyti.academic.domain.model.ProfessorType professorType;
        String employeeNumber;
        String email;
        String firstName;
        String firstLastName;
        String secondLastName;
        String phone;
        String phoneExtension;
        boolean commissionMember;
        java.time.LocalDate nextSabbaticalStart;
        java.time.LocalDate nextSabbaticalEnd;
        Long graduateProgramId;
        String generatedPassword;
    }
}

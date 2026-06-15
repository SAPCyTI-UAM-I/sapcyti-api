package mx.uam.sapcyti.academic.application.service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.academic.application.command.RegisterProfessorCommand;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEmployeeNumberException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateProfessorEmailException;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.service.PasswordGenerationService;
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

        String normalizedEmail = command.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateProfessorEmailException();
        }
        if (professorRepository.existsByEmployeeNumber(command.employeeNumber().trim())) {
            throw new DuplicateEmployeeNumberException();
        }

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
                null);

        Professor professor = new Professor(
                command.employeeNumber().trim(),
                user.getId(),
                command.graduateProgramId(),
                personalData);
        professor = professorRepository.save(professor);

        return RegisterProfessorResult.builder()
                .id(professor.getId())
                .userId(user.getId())
                .employeeNumber(professor.getEmployeeNumber())
                .email(normalizedEmail)
                .firstName(personalData.getFirstName())
                .firstLastName(personalData.getFirstLastName())
                .secondLastName(personalData.getSecondLastName())
                .graduateProgramId(professor.getGraduateProgramId())
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
        String employeeNumber;
        String email;
        String firstName;
        String firstLastName;
        String secondLastName;
        Long graduateProgramId;
        String generatedPassword;
    }
}

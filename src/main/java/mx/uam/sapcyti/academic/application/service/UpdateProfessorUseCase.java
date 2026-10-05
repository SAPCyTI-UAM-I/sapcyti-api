package mx.uam.sapcyti.academic.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.command.UpdateProfessorCommand;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEmployeeNumberException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateProfessorEmailException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorInformation;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorTrimestralAssignmentsPort;
import mx.uam.sapcyti.academic.domain.service.ProfessorTypeRules;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProfessorUseCase {

    private final ProfessorRepositoryPort professorRepository;
    private final UserRepositoryPort userRepository;
    private final ProfessorTrimestralAssignmentsPort trimestralAssignmentsPort;

    @Transactional
    public ListProfessorsUseCase.ProfessorListItem execute(UpdateProfessorCommand command) {
        Long graduateProgramId = requireTenant();
        assertValidSabbaticalPeriod(command.nextSabbaticalStart(), command.nextSabbaticalEnd());

        Professor professor = professorRepository.findByIdAndGraduateProgramId(
                        command.professorId(), graduateProgramId)
                .orElseThrow(ProfessorNotFoundException::new);

        User user = userRepository.findById(professor.getUserId())
                .orElseThrow(ProfessorNotFoundException::new);

        ProfessorTypeRules.assertUpdateAllowed(
                professor.getProfessorType(),
                professor.getEmployeeNumber(),
                command.professorType(),
                command.employeeNumber());

        String normalizedEmployeeNumber = ProfessorTypeRules.normalizeEmployeeNumber(
                command.professorType(), command.employeeNumber());
        assertUniqueEmployeeNumber(
                graduateProgramId, normalizedEmployeeNumber, professor.getId());
        assertUniqueEmail(command.email(), user.getId());

        String normalizedEmail = command.email().trim().toLowerCase();
        if (!normalizedEmail.equals(user.getEmail())) {
            user.setEmail(normalizedEmail);
            userRepository.save(user);
        }

        PersonalData personalData = new PersonalData(
                command.firstName().trim(),
                command.firstLastName().trim(),
                blankToNull(command.secondLastName()),
                professor.getPersonalData().getNationality(),
                professor.getPersonalData().getBirthDate(),
                command.phone().trim(),
                blankToNull(command.phoneExtension()));

        ProfessorInformation professorInformation = new ProfessorInformation(
                command.commissionMember(),
                command.nextSabbaticalStart(),
                command.nextSabbaticalEnd());

        professor.updatePersonalData(personalData);
        professor.updateProfessorInformation(professorInformation);
        professor.updateTypeAndEmployeeNumber(command.professorType(), normalizedEmployeeNumber);
        professor = professorRepository.save(professor);
        trimestralAssignmentsPort.refreshOpenPlanSnapshots(
                professor.getId(),
                graduateProgramId,
                professor.getEmployeeNumber(),
                fullName(professor.getPersonalData()));

        return ListProfessorsUseCase.toListItem(professor, user);
    }

    private void assertUniqueEmail(String email, Long currentUserId) {
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new DuplicateProfessorEmailException();
                });
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

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
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

    private static String fullName(PersonalData personalData) {
        String secondLastName = blankToNull(personalData.getSecondLastName());
        return secondLastName == null
                ? personalData.getFirstName() + " " + personalData.getFirstLastName()
                : personalData.getFirstName() + " " + personalData.getFirstLastName()
                        + " " + secondLastName;
    }
}

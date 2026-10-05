package mx.uam.sapcyti.academic.application.command;

import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;

/**
 * Command to register a new professor (HU-21).
 */
public record RegisterProfessorCommand(
        ProfessorType professorType,
        String employeeNumber,
        String email,
        Long graduateProgramId,
        String firstName,
        String firstLastName,
        String secondLastName,
        String phone,
        String phoneExtension,
        boolean commissionMember,
        LocalDate nextSabbaticalStart,
        LocalDate nextSabbaticalEnd) {
}

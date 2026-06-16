package mx.uam.sapcyti.academic.application.command;

import java.time.LocalDate;

/**
 * Command to register a new professor (HU-21).
 */
public record RegisterProfessorCommand(
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

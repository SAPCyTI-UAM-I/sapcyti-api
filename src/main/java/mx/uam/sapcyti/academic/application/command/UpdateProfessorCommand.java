package mx.uam.sapcyti.academic.application.command;

import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;

/**
 * Command to update an existing professor (HU-24).
 */
public record UpdateProfessorCommand(
        Long professorId,
        ProfessorType professorType,
        String employeeNumber,
        String email,
        String firstName,
        String firstLastName,
        String secondLastName,
        String phone,
        String phoneExtension,
        boolean commissionMember,
        LocalDate nextSabbaticalStart,
        LocalDate nextSabbaticalEnd) {
}

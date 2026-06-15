package mx.uam.sapcyti.academic.application.command;

import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Command to register a new student (HU-15).
 */
public record RegisterStudentCommand(
        String enrollmentId,
        String email,
        Long graduateProgramId,
        Long advisorId,
        String firstName,
        String firstLastName,
        String secondLastName,
        String nationality,
        String undergraduateDegree,
        ProgramType programType,
        LocalDate admissionDate) {
}

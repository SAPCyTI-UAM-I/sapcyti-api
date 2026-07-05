package mx.uam.sapcyti.academic.application.command;

import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Command to update student personal and student-level academic fields (HU-18).
 */
public record UpdateStudentCommand(
        Long studentId,
        String firstName,
        String firstLastName,
        String secondLastName,
        String email,
        String nationality,
        LocalDate birthDate,
        String phone,
        String phoneExtension,
        String undergraduateDegree,
        DegreeLevel lastDegreeObtained,
        ProgramType programType,
        LocalDate admissionDate,
        boolean active) {
}

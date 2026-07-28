package mx.uam.sapcyti.academic.application.command;

import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Command to register a new student (HU-15 / HU-56).
 */
public record RegisterStudentCommand(
        String enrollmentId,
        String email,
        Long graduateProgramId,
        Long advisorId,
        java.util.List<Long> advisorIds,
        Long tutorId,
        String lineOfKnowledge,
        String researchArea,
        String firstName,
        String firstLastName,
        String secondLastName,
        String nationality,
        LocalDate birthDate,
        String phone,
        String phoneExtension,
        String undergraduateDegree,
        DegreeLevel lastDegreeObtained,
        ProgramType programType,
        LocalDate admissionDate,
        String admissionTerm) {
}

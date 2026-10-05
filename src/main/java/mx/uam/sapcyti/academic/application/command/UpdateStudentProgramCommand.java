package mx.uam.sapcyti.academic.application.command;

import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;

/**
 * Command to update an existing student program (HU-20).
 */
public record UpdateStudentProgramCommand(
        Long studentId,
        Long programId,
        LocalDate admissionDate,
        LocalDate graduationDate,
        String lineOfKnowledge,
        String researchArea,
        ProgramStatus status,
        String withdrawalReason,
        Long tutorId,
        List<Long> advisorIds) {
}

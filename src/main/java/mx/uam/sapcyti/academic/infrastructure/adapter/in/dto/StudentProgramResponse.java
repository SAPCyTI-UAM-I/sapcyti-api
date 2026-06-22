package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Read model for a student program including resolved tutor and advisor names (HU-19).
 */
public record StudentProgramResponse(
        Long id,
        Long studentId,
        Long graduateProgramId,
        String enrollmentId,
        ProgramType programType,
        LocalDate admissionDate,
        LocalDate graduationDate,
        String researchArea,
        ProgramStatus status,
        String withdrawalReason,
        Long tutorId,
        ProfessorReferenceResponse tutor,
        List<Long> advisorIds,
        List<ProfessorReferenceResponse> advisors) {
}

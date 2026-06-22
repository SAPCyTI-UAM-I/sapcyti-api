package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Lightweight read model for listing student programs (HU-19).
 */
public record StudentProgramSummaryResponse(
        Long id,
        ProgramType programType,
        String enrollmentId,
        ProgramStatus status,
        Long tutorId,
        boolean hasTutor) {
}

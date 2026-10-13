package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import java.util.List;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.EligibilityReason;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.ProgramStatus;

/**
 * Body of {@code GET /api/exam-requests/me/{examType}}.
 * {@code request} is null when the student has no draft. The call does not create one.
 */
public record ExamPanelResponse(
        ExamType examType,
        Degree degree,
        ProgramStatus programStatus,
        boolean eligible,
        EligibilityReason eligibilityReason,
        ExamRequestViewResponse request,
        List<ExamRequirementResponse> requirements,
        int requiredPendingCount) {
}

package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import java.util.List;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.EligibilityReason;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.ProgramStatus;

/**
 * Body of {@code GET /api/exam-requests/me/{examType}/requirements}.
 * {@code request} is null when the student has no binding.
 */
public record ExamRequirementsResponse(
        ExamType examType,
        Degree degree,
        ProgramStatus programStatus,
        boolean eligible,
        EligibilityReason eligibilityReason,
        ExamRequestSummaryResponse request,
        List<ExamRequirementResponse> requirements) {

    public static ExamRequirementsResponse from(ExamRequirements requirements) {
        ExamRequestSummaryResponse summary = requirements.request() == null
                ? null
                : ExamRequestSummaryResponse.from(requirements.request());
        return new ExamRequirementsResponse(
                requirements.examType(),
                requirements.degree(),
                requirements.programStatus(),
                requirements.eligible(),
                requirements.eligibilityReason(),
                summary,
                requirements.requirements().stream().map(ExamRequirementResponse::from).toList());
    }
}

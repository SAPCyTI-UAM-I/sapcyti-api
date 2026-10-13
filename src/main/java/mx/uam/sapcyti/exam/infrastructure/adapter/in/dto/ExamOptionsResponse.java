package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import java.util.List;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.EligibilityReason;
import mx.uam.sapcyti.exam.domain.model.ExamOptions;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.ProgramStatus;

/**
 * Body of {@code GET /api/exam-requests/me/options}. Null {@code eligibilityReason} is published.
 */
public record ExamOptionsResponse(
        Degree degree,
        ProgramStatus programStatus,
        boolean eligible,
        EligibilityReason eligibilityReason,
        List<ExamType> examTypes) {

    public static ExamOptionsResponse from(ExamOptions options) {
        return new ExamOptionsResponse(
                options.degree(),
                options.programStatus(),
                options.eligible(),
                options.eligibilityReason(),
                options.examTypes());
    }
}

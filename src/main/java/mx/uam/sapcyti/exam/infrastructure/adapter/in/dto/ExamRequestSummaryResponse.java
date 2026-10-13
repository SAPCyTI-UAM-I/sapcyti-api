package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import java.time.Instant;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamRequestSummary;
import mx.uam.sapcyti.exam.domain.model.ExamType;

/**
 * Summary embedded when a binding still matches the current academic context.
 */
public record ExamRequestSummaryResponse(
        Long id,
        ExamType examType,
        Degree degree,
        ExamRequestStatus status,
        String folio,
        int version,
        Instant submittedAt) {

    public static ExamRequestSummaryResponse from(ExamRequestSummary summary) {
        return new ExamRequestSummaryResponse(
                summary.id(),
                summary.examType(),
                summary.degree(),
                summary.status(),
                summary.folio(),
                summary.version(),
                summary.submittedAt());
    }
}

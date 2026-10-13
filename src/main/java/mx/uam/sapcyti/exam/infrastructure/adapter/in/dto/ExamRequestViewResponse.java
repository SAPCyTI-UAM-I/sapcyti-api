package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import java.time.Instant;
import java.util.List;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamType;

/**
 * Request embedded in a draft panel or mutation. {@code delivery} stays null.
 * No object key, bucket or hash.
 */
public record ExamRequestViewResponse(
        Long id,
        ExamType examType,
        Degree degree,
        ExamRequestStatus status,
        String folio,
        int version,
        Instant submittedAt,
        List<ExamRequirementResponse> requirements,
        Object delivery) {
}

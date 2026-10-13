package mx.uam.sapcyti.exam.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Summary embedded when a binding still matches the current academic context
 * (exam-requests.schema.json#/definitions/ExamRequestSummary). Null on the wire when absent.
 */
public record ExamRequestSummary(
        Long id,
        ExamType examType,
        Degree degree,
        ExamRequestStatus status,
        String folio,
        int version,
        Instant submittedAt) {

    public ExamRequestSummary {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        Objects.requireNonNull(examType, "examType");
        Objects.requireNonNull(degree, "degree");
        Objects.requireNonNull(status, "status");
        if (version < 1) {
            throw new IllegalArgumentException("version must be at least 1");
        }
        if (status == ExamRequestStatus.BORRADOR) {
            if (folio != null || submittedAt != null) {
                throw new IllegalArgumentException("BORRADOR has no folio or submittedAt");
            }
        } else if (folio == null || folio.isBlank() || submittedAt == null) {
            throw new IllegalArgumentException("folio and submittedAt are required after BORRADOR");
        }
    }
}

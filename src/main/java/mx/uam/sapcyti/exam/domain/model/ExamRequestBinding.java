package mx.uam.sapcyti.exam.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Facts already stored for one student and exam type
 * (exam-requests.schema.json#/definitions/ExamRequestBinding).
 * HU-62 compares them with the current context and does not insert a row.
 */
public record ExamRequestBinding(
        Long id,
        Long studentId,
        ExamType examType,
        Long graduateProgramId,
        Degree degree,
        ExamRequestStatus status,
        String folio,
        int version,
        Instant submittedAt) {

    public ExamRequestBinding {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(examType, "examType");
        Objects.requireNonNull(graduateProgramId, "graduateProgramId");
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

package mx.uam.sapcyti.exam.domain.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.StoredDocumentReference;

/**
 * Confirms draft references, the version and superseded PDF marks in one database transaction.
 * It does not open that transaction around the object write. An empty document list is not an insert.
 */
public interface ExamRequestDraftPort {

    /**
     * Next request id. Does not insert a row. Call this before the object put.
     */
    long reserveRequestId();

    /**
     * Next document id. Does not insert a row. Call this before the object put.
     */
    long reserveDocumentId();

    /**
     * Inserts the first {@code BORRADOR} row and its current references. Stored version is 1.
     * {@code folio} and {@code submittedAt} stay null. A second {@code (studentId, examType)} does not insert.
     */
    ExamRequestRead create(Creation creation);

    /**
     * Adds or replaces current references on an existing draft and increments the version once.
     * A replaced PDF mark is written in the same transaction. A stale version or a status other than
     * {@code BORRADOR} changes nothing.
     */
    ExamRequestRead save(Save save);

    /**
     * Removes the current reference of one type. A PDF mark is written in the same transaction.
     * The version increments once. A missing request does not insert a draft.
     */
    ExamRequestRead delete(Deletion deletion);

    /**
     * First draft. {@code requestId} and each document id were reserved before the put.
     */
    record Creation(
            long requestId,
            long studentId,
            ExamType examType,
            long graduateProgramId,
            Degree degree,
            List<StoredDocumentReference> documents) {

        public Creation {
            if (requestId < 1 || studentId < 1 || graduateProgramId < 1) {
                throw new IllegalArgumentException("ids must be positive");
            }
            Objects.requireNonNull(examType, "examType");
            Objects.requireNonNull(degree, "degree");
            if (documents == null || documents.isEmpty()) {
                throw new IllegalArgumentException("a draft is not created without a document");
            }
            documents = List.copyOf(documents);
        }
    }

    /**
     * One batch against the stored version. Empty batches are rejected and do not bump the version.
     */
    record Save(
            long studentId,
            ExamType examType,
            int expectedVersion,
            List<StoredDocumentReference> documents) {

        public Save {
            if (studentId < 1) {
                throw new IllegalArgumentException("studentId must be positive");
            }
            Objects.requireNonNull(examType, "examType");
            if (expectedVersion < 1) {
                throw new IllegalArgumentException("expected version must be at least 1");
            }
            if (documents == null || documents.isEmpty()) {
                throw new IllegalArgumentException("an empty batch does not change the draft");
            }
            documents = List.copyOf(documents);
        }
    }

    /**
     * Deletes one current type when {@code expectedVersion} is the stored version.
     */
    record Deletion(
            long studentId,
            ExamType examType,
            int expectedVersion,
            DocumentType documentType,
            Instant supersededAt) {

        public Deletion {
            if (studentId < 1) {
                throw new IllegalArgumentException("studentId must be positive");
            }
            Objects.requireNonNull(examType, "examType");
            if (expectedVersion < 1) {
                throw new IllegalArgumentException("expected version must be at least 1");
            }
            Objects.requireNonNull(documentType, "documentType");
            Objects.requireNonNull(supersededAt, "supersededAt");
        }
    }
}

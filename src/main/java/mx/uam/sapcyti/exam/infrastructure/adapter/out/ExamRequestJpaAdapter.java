package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mx.uam.sapcyti.exam.domain.exception.ExamDocumentNotFoundException;
import mx.uam.sapcyti.exam.domain.exception.ExamDraftConflictException;
import mx.uam.sapcyti.exam.domain.exception.ExamDraftNotFoundException;
import mx.uam.sapcyti.exam.domain.exception.ExamRequestNotDraftException;
import mx.uam.sapcyti.exam.domain.exception.StaleExamDraftException;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamObjectKey;
import mx.uam.sapcyti.exam.domain.model.ExamRequestBinding;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.StoredDocumentReference;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestDraftPort;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production {@link ExamRequestReadPort} and {@link ExamRequestDraftPort}.
 * Do not register a second read adapter beside this one.
 *
 * <p>The object write is not part of this transaction. A failed flush leaves the previous
 * reference and writes no superseded mark. {@code noticeUncommitted} belongs to the storage port.
 */
@Repository
public class ExamRequestJpaAdapter implements ExamRequestReadPort, ExamRequestDraftPort {

    static final String REQUEST_SEQUENCE = "exam_requests_id_seq";
    static final String DOCUMENT_SEQUENCE = "exam_request_documents_id_seq";

    private final SpringDataExamRequestRepository requests;
    private final SpringDataExamDocumentRepository documentRows;

    @PersistenceContext
    private EntityManager entityManager;

    public ExamRequestJpaAdapter(
            SpringDataExamRequestRepository requests,
            SpringDataExamDocumentRepository documentRows) {
        this.requests = requests;
        this.documentRows = documentRows;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamRequestRead> findByStudentAndType(Long studentId, ExamType examType) {
        return requests.findByStudentIdAndExamType(studentId, examType).map(this::toRead);
    }

    @Override
    @Transactional
    public long reserveRequestId() {
        return nextValue(REQUEST_SEQUENCE);
    }

    @Override
    @Transactional
    public long reserveDocumentId() {
        return nextValue(DOCUMENT_SEQUENCE);
    }

    @Override
    @Transactional
    public ExamRequestRead create(Creation creation) {
        validateDocuments(creation.graduateProgramId(), creation.studentId(), creation.requestId(), creation.documents());
        ExamRequestEntity request = ExamRequestEntity.draft(
                creation.requestId(),
                creation.studentId(),
                creation.examType(),
                creation.graduateProgramId(),
                creation.degree());
        try {
            entityManager.persist(request);
            for (StoredDocumentReference document : creation.documents()) {
                entityManager.persist(ExamDocumentEntity.from(document));
            }
            entityManager.flush();
        } catch (RuntimeException ex) {
            if (isUniqueViolation(ex)) {
                throw new ExamDraftConflictException();
            }
            throw ex;
        }
        return toRead(request);
    }

    @Override
    @Transactional
    public ExamRequestRead save(Save save) {
        ExamRequestEntity request = requireMutable(save.studentId(), save.examType(), save.expectedVersion());
        validateDocuments(
                request.getGraduateProgramId(), request.getStudentId(), request.getId(), save.documents());
        for (StoredDocumentReference document : save.documents()) {
            replaceOrAdd(request, document);
        }
        request.incrementVersion();
        entityManager.flush();
        return toRead(request);
    }

    @Override
    @Transactional
    public ExamRequestRead delete(Deletion deletion) {
        ExamRequestEntity request = requireMutable(
                deletion.studentId(), deletion.examType(), deletion.expectedVersion());
        ExamDocumentEntity current = documentRows
                .findByExamRequestIdAndDocumentType(request.getId(), deletion.documentType())
                .orElseThrow(ExamDocumentNotFoundException::new);
        if (current.getKind() == DocumentKind.PDF) {
            entityManager.persist(SupersededObjectEntity.of(
                    current.getObjectKey(), current.getSha256(), deletion.supersededAt()));
        }
        documentRows.delete(current);
        request.incrementVersion();
        entityManager.flush();
        return toRead(request);
    }

    private ExamRequestEntity requireMutable(long studentId, ExamType examType, int expectedVersion) {
        ExamRequestEntity request = requests.lockByStudentAndType(studentId, examType)
                .orElseThrow(ExamDraftNotFoundException::new);
        if (request.getVersion() != expectedVersion) {
            throw new StaleExamDraftException(request.getVersion());
        }
        if (request.getStatus() != ExamRequestStatus.BORRADOR) {
            throw new ExamRequestNotDraftException();
        }
        return request;
    }

    private void validateDocuments(
            long graduateProgramId,
            long studentId,
            long requestId,
            List<StoredDocumentReference> incoming) {
        Set<DocumentType> seen = new HashSet<>();
        for (StoredDocumentReference document : incoming) {
            if (!seen.add(document.documentType())) {
                throw new IllegalArgumentException("document type is repeated");
            }
            if (document.examRequestId() != requestId) {
                throw new IllegalArgumentException("document does not belong to the request");
            }
            if (document.kind() == DocumentKind.PDF) {
                String expected = ExamObjectKey.pdf(graduateProgramId, studentId, requestId, document.id());
                if (!expected.equals(document.objectKey())) {
                    throw new IllegalArgumentException("object key does not match the reserved ids");
                }
            }
            documentRows.findByExamRequestIdAndDocumentType(requestId, document.documentType())
                    .filter(current -> current.getId().equals(document.id()))
                    .ifPresent(current -> {
                        throw new IllegalArgumentException("replacement requires a new document id");
                    });
        }
    }

    private void replaceOrAdd(ExamRequestEntity request, StoredDocumentReference document) {
        Optional<ExamDocumentEntity> current = documentRows.findByExamRequestIdAndDocumentType(
                request.getId(), document.documentType());
        if (current.isPresent()) {
            ExamDocumentEntity previous = current.get();
            if (previous.getKind() == DocumentKind.PDF) {
                entityManager.persist(SupersededObjectEntity.of(
                        previous.getObjectKey(), previous.getSha256(), document.uploadedAt()));
            }
            documentRows.delete(previous);
            entityManager.flush();
        }
        entityManager.persist(ExamDocumentEntity.from(document));
    }

    private ExamRequestRead toRead(ExamRequestEntity request) {
        List<CurrentExamDocument> documents = documentRows.findByExamRequestIdOrderByIdAsc(request.getId()).stream()
                .map(ExamRequestJpaAdapter::toCurrent)
                .toList();
        return new ExamRequestRead(toBinding(request), documents);
    }

    private static ExamRequestBinding toBinding(ExamRequestEntity entity) {
        return new ExamRequestBinding(
                entity.getId(),
                entity.getStudentId(),
                entity.getExamType(),
                entity.getGraduateProgramId(),
                entity.getDegree(),
                entity.getStatus(),
                entity.getFolio(),
                entity.getVersion(),
                entity.getSubmittedAt());
    }

    private static CurrentExamDocument toCurrent(ExamDocumentEntity entity) {
        return new CurrentExamDocument(
                entity.getId(),
                entity.getDocumentType(),
                entity.getKind(),
                entity.getOriginalName(),
                entity.getExternalUrl(),
                entity.getSizeBytes(),
                entity.getUploadedAt());
    }

    private long nextValue(String sequence) {
        Object value = entityManager.createNativeQuery("SELECT nextval('" + sequence + "')").getSingleResult();
        return ((Number) value).longValue();
    }

    private static boolean isUniqueViolation(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraint && uniqueState(constraint.getSQLException())) {
                return true;
            }
            if (current instanceof SQLException sql && uniqueState(sql)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean uniqueState(SQLException sql) {
        return sql != null && "23505".equals(sql.getSQLState());
    }
}

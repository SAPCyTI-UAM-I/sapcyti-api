package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import jakarta.persistence.EntityManager;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import mx.uam.sapcyti.exam.domain.exception.ExamDocumentNotFoundException;
import mx.uam.sapcyti.exam.domain.exception.ExamDraftConflictException;
import mx.uam.sapcyti.exam.domain.exception.ExamDraftNotFoundException;
import mx.uam.sapcyti.exam.domain.exception.ExamRequestNotDraftException;
import mx.uam.sapcyti.exam.domain.exception.ObjectStorageException;
import mx.uam.sapcyti.exam.domain.exception.StaleExamDraftException;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamObjectKey;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.PrivateObjectPut;
import mx.uam.sapcyti.exam.domain.model.StoredDocumentReference;
import mx.uam.sapcyti.exam.domain.model.StoredObjectReceipt;
import mx.uam.sapcyti.exam.domain.model.UncommittedObjectNotice;
import mx.uam.sapcyti.exam.domain.port.out.DocumentStoragePort;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestDraftPort.Creation;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestDraftPort.Deletion;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestDraftPort.Save;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SPEC-051: the draft row, the current reference and the superseded mark.
 * The storage port is a double. HTTP status belongs to SPEC-052.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:exam_draft;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
})
@Import(ExamRequestJpaAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExamRequestJpaAdapterTest {

    private static final long STUDENT_ID = 10L;
    private static final long OTHER_STUDENT_ID = 11L;
    private static final long PROGRAM_ID = 17L;
    private static final long OTHER_PROGRAM_ID = 99L;
    private static final Instant UPLOADED_AT = Instant.parse("2026-10-12T18:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-12T19:00:00Z");
    private static final String SHA_A = "a".repeat(64);
    private static final String SHA_B = "b".repeat(64);
    private static final String TESIS_NAME = "Tesis_Mtria_Lopez_Alba_Emanuel.pdf";
    private static final String SOL_NAME = "Sol_Sinodales_Lopez_Alba_Emanuel.pdf";
    private static final String ARTICLE_URL = "https://revista.ejemplo.org/articulo/123";

    @Autowired
    private ExamRequestJpaAdapter adapter;

    @Autowired
    private SpringDataExamRequestRepository requests;

    @Autowired
    private SpringDataExamDocumentRepository documentRows;

    @Autowired
    private SpringDataSupersededObjectRepository marks;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private RecordingStorage storage;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        storage = new RecordingStorage();
        reset();
    }

    @AfterEach
    void tearDown() {
        reset();
    }

    @Test
    @DisplayName("a valid selection creates the draft and the read hides key, bucket and hash")
    void validSelectionCreatesDraftAndReadHidesStorage() {
        assertThat(adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO)).isEmpty();
        assertThat(requests.count()).isZero();

        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        ExamRequestRead saved = adapter.create(grado(tesis));

        assertThat(saved.binding().status()).isEqualTo(ExamRequestStatus.BORRADOR);
        assertThat(saved.binding().version()).isEqualTo(1);
        assertThat(saved.binding().graduateProgramId()).isEqualTo(PROGRAM_ID);
        assertThat(saved.binding().degree()).isEqualTo(Degree.MAESTRIA);
        assertThat(saved.documents()).singleElement().satisfies(document -> {
            assertThat(document.id()).isEqualTo(tesis.id());
            assertThat(document.documentType()).isEqualTo(DocumentType.TESIS);
            assertThat(document.kind()).isEqualTo(DocumentKind.PDF);
            assertThat(document.originalName()).isEqualTo(TESIS_NAME);
            assertThat(document.sizeBytes()).isEqualTo(1200L);
            assertThat(document.uploadedAt()).isEqualTo(UPLOADED_AT);
            assertThat(document.externalUrl()).isNull();
        });
        assertThat(saved.toString()).doesNotContain(tesis.objectKey()).doesNotContain(SHA_A);
        assertThat(documentRows.findAll()).singleElement().satisfies(row -> {
            assertThat(row.getObjectKey()).isEqualTo(tesis.objectKey());
            assertThat(row.getObjectKey()).isEqualTo(ExamObjectKey.pdf(PROGRAM_ID, STUDENT_ID, tesis.examRequestId(), tesis.id()));
            assertThat(row.getObjectKey()).doesNotContain(TESIS_NAME);
            assertThat(row.getSha256()).isEqualTo(SHA_A);
        });

        ExamRequestRead again = adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO).orElseThrow();
        assertThat(again.documents()).extracting(CurrentExamDocument::id).containsExactly(tesis.id());
        assertThat(again.toString()).doesNotContain(tesis.objectKey()).doesNotContain(SHA_A);
        assertThat(requests.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("saving does not submit the request")
    void savingDoesNotSubmit() {
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        ExamRequestRead saved = adapter.create(grado(tesis));

        assertThat(saved.binding().status()).isEqualTo(ExamRequestStatus.BORRADOR);
        assertThat(saved.binding().folio()).isNull();
        assertThat(saved.binding().submittedAt()).isNull();

        StoredDocumentReference other = putPdf(
                OTHER_STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        adapter.create(new Creation(
                other.examRequestId(), OTHER_STUDENT_ID, ExamType.GRADO, PROGRAM_ID, Degree.MAESTRIA, List.of(other)));

        assertThat(requests.findAll()).allSatisfy(row -> {
            assertThat(row.getFolio()).isNull();
            assertThat(row.getSubmittedAt()).isNull();
            assertThat(row.getStatus()).isEqualTo(ExamRequestStatus.BORRADOR);
        });
    }

    @Test
    @DisplayName("several saved documents in one batch increment the version once")
    void severalDocumentsIncrementVersionOnce() {
        long requestId = reserveRequest();
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, requestId, DocumentType.TESIS, TESIS_NAME, SHA_A);
        StoredDocumentReference sol = putPdf(STUDENT_ID, PROGRAM_ID, requestId, DocumentType.SOL_SINODALES, SOL_NAME, SHA_B);
        ExamRequestRead saved = adapter.create(grado(tesis, sol));

        assertThat(saved.binding().version()).isEqualTo(1);
        assertThat(saved.documents()).extracting(CurrentExamDocument::documentType)
                .containsExactly(DocumentType.TESIS, DocumentType.SOL_SINODALES);
        assertThat(marks.count()).isZero();
        assertThat(storage.puts).hasSize(2);
    }

    @Test
    @DisplayName("an article URL is saved without an object")
    void articleUrlIsSavedWithoutAnObject() {
        long requestId = reserveRequest();
        long documentId = adapter.reserveDocumentId();
        StoredDocumentReference article = new StoredDocumentReference(
                documentId,
                requestId,
                DocumentType.ARTICULO,
                DocumentKind.URL,
                null,
                null,
                null,
                null,
                ARTICLE_URL,
                null,
                UPLOADED_AT);
        ExamRequestRead saved = adapter.create(new Creation(
                requestId, STUDENT_ID, ExamType.GRADO, PROGRAM_ID, Degree.DOCTORADO, List.of(article)));

        assertThat(storage.puts).isEmpty();
        assertThat(marks.count()).isZero();
        assertThat(saved.documents()).singleElement().satisfies(document -> {
            assertThat(document.kind()).isEqualTo(DocumentKind.URL);
            assertThat(document.externalUrl()).isEqualTo(ARTICLE_URL);
            assertThat(document.originalName()).isNull();
            assertThat(document.sizeBytes()).isNull();
        });
        assertThat(documentRows.findAll()).singleElement().satisfies(row -> assertThat(row.getObjectKey()).isNull());

        adapter.delete(new Deletion(STUDENT_ID, ExamType.GRADO, 1, DocumentType.ARTICULO, LATER));
        assertThat(marks.count()).isZero();
        assertThat(adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO).orElseThrow().documents()).isEmpty();
        assertThat(adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO).orElseThrow().binding().version()).isEqualTo(2);
    }

    @Test
    @DisplayName("a failed object write keeps the document whose put finished and skips the one without a receipt")
    void failedObjectWriteKeepsTheOthers() {
        long requestId = reserveRequest();
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, requestId, DocumentType.TESIS, TESIS_NAME, SHA_A);
        long histId = adapter.reserveDocumentId();
        String histKey = ExamObjectKey.pdf(PROGRAM_ID, STUDENT_ID, requestId, histId);
        storage.fail(histKey);
        assertThatThrownBy(() -> storage.put(
                new PrivateObjectPut(histKey, SHA_B, StoredDocumentReference.PDF_MEDIA_TYPE, 10L),
                InputStream.nullInputStream()))
                .isInstanceOf(ObjectStorageException.class);

        ExamRequestRead saved = adapter.create(grado(tesis));

        assertThat(saved.documents()).extracting(CurrentExamDocument::documentType).containsExactly(DocumentType.TESIS);
        assertThat(documentRows.findAll()).extracting(ExamDocumentEntity::getDocumentType).containsExactly(DocumentType.TESIS);
        assertThat(saved.binding().version()).isEqualTo(1);
    }

    @Test
    @DisplayName("a batch in which every document fails creates no draft")
    void everyFailureCreatesNoDraft() {
        long requestId = reserveRequest();
        long documentId = adapter.reserveDocumentId();
        String key = ExamObjectKey.pdf(PROGRAM_ID, STUDENT_ID, requestId, documentId);
        storage.fail(key);
        assertThatThrownBy(() -> storage.put(
                new PrivateObjectPut(key, SHA_A, StoredDocumentReference.PDF_MEDIA_TYPE, 10L),
                InputStream.nullInputStream()))
                .isInstanceOf(ObjectStorageException.class);

        assertThatThrownBy(() -> new Creation(requestId, STUDENT_ID, ExamType.GRADO, PROGRAM_ID, Degree.MAESTRIA, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(requests.count()).isZero();
        assertThat(documentRows.count()).isZero();
    }

    @Test
    @DisplayName("a failed document does not replace the reference already stored")
    void failedDocumentDoesNotReplaceTheStoredReference() {
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        ExamRequestRead saved = adapter.create(grado(tesis));
        int putsBefore = storage.puts.size();

        ExamRequestRead again = adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO).orElseThrow();
        assertThat(again.binding().version()).isEqualTo(saved.binding().version());
        assertThat(again.documents()).extracting(CurrentExamDocument::id).containsExactly(tesis.id());
        assertThat(storage.puts).hasSize(putsBefore);
        assertThat(marks.count()).isZero();
    }

    @Test
    @DisplayName("a database commit failure publishes no saved result and notices the new key")
    void databaseCommitFailureLeavesThePreviousReference() {
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        adapter.create(grado(tesis));
        StoredDocumentReference replacement = putPdf(
                STUDENT_ID, PROGRAM_ID, tesis.examRequestId(), DocumentType.TESIS, TESIS_NAME, SHA_B);

        rejectMarks();
        try {
            assertThatThrownBy(() -> adapter.save(new Save(STUDENT_ID, ExamType.GRADO, 1, List.of(replacement))))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            dropMarkRejection();
        }

        UncommittedObjectNotice notice = storage.noticeUncommitted(replacement.objectKey(), replacement.sha256());
        assertThat(notice.objectKey()).isEqualTo(replacement.objectKey());
        assertThat(storage.notices).containsExactly(notice);
        assertThat(marks.count()).isZero();

        ExamRequestRead current = adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO).orElseThrow();
        assertThat(current.binding().version()).isEqualTo(1);
        assertThat(current.documents()).extracting(CurrentExamDocument::id).containsExactly(tesis.id());
        assertThat(documentRows.findAll()).singleElement().satisfies(row -> {
            assertThat(row.getObjectKey()).isEqualTo(tesis.objectKey());
            assertThat(row.getSha256()).isEqualTo(SHA_A);
        });
    }

    @Test
    @DisplayName("deleting a draft document makes that type pending and marks the previous key")
    void deletingADraftDocumentMarksThePreviousKey() {
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        adapter.create(grado(tesis));

        ExamRequestRead deleted = adapter.delete(new Deletion(STUDENT_ID, ExamType.GRADO, 1, DocumentType.TESIS, LATER));

        assertThat(deleted.binding().version()).isEqualTo(2);
        assertThat(deleted.binding().folio()).isNull();
        assertThat(deleted.binding().submittedAt()).isNull();
        assertThat(deleted.documents()).isEmpty();
        assertThat(deleted.toString()).doesNotContain(tesis.objectKey()).doesNotContain(SHA_A);
        assertThat(marks.findAll()).singleElement().satisfies(mark -> {
            assertThat(mark.getObjectKey()).isEqualTo(tesis.objectKey());
            assertThat(mark.getSha256()).isEqualTo(SHA_A);
            assertThat(mark.getSupersededAt()).isEqualTo(LATER);
        });
    }

    @Test
    @DisplayName("a failed delete transaction leaves no mark and keeps the reference")
    void failedDeleteLeavesNoMark() {
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        adapter.create(grado(tesis));
        rejectMarks();
        try {
            assertThatThrownBy(() -> adapter.delete(new Deletion(STUDENT_ID, ExamType.GRADO, 1, DocumentType.TESIS, LATER)))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            dropMarkRejection();
        }

        assertThat(marks.count()).isZero();
        ExamRequestRead current = adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO).orElseThrow();
        assertThat(current.binding().version()).isEqualTo(1);
        assertThat(current.documents()).extracting(CurrentExamDocument::id).containsExactly(tesis.id());
    }

    @Test
    @DisplayName("replacing a draft document leaves only the new reference and marks the previous key")
    void replacingADraftDocumentLeavesOnlyTheNewReference() {
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        adapter.create(grado(tesis));
        StoredDocumentReference replacement = putPdf(
                STUDENT_ID, PROGRAM_ID, tesis.examRequestId(), DocumentType.TESIS, TESIS_NAME, SHA_B);

        ExamRequestRead saved = adapter.save(new Save(STUDENT_ID, ExamType.GRADO, 1, List.of(replacement)));

        assertThat(saved.binding().version()).isEqualTo(2);
        assertThat(saved.binding().folio()).isNull();
        assertThat(saved.documents()).singleElement().satisfies(document -> {
            assertThat(document.id()).isEqualTo(replacement.id());
            assertThat(document.originalName()).isEqualTo(TESIS_NAME);
        });
        assertThat(saved.toString()).doesNotContain(tesis.objectKey()).doesNotContain(replacement.objectKey()).doesNotContain(SHA_B);
        assertThat(marks.findAll()).singleElement().satisfies(mark -> {
            assertThat(mark.getObjectKey()).isEqualTo(tesis.objectKey());
            assertThat(mark.getSha256()).isEqualTo(SHA_A);
            assertThat(mark.getSupersededAt()).isEqualTo(UPLOADED_AT);
        });
        assertThat(documentRows.findAll()).singleElement().extracting(ExamDocumentEntity::getId).isEqualTo(replacement.id());
    }

    @Test
    @DisplayName("a rejected replacement keeps the previous reference")
    void rejectedReplacementDoesNotCallTheWritePort() {
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        adapter.create(grado(tesis));

        ExamRequestRead current = adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO).orElseThrow();
        assertThat(current.binding().version()).isEqualTo(1);
        assertThat(current.documents()).extracting(CurrentExamDocument::id).containsExactly(tesis.id());
        assertThat(marks.count()).isZero();
        assertThat(storage.puts).hasSize(1);
    }

    @Test
    @DisplayName("replacing or deleting a sent request is rejected")
    void sentRequestIsNotMutated() {
        long requestId = reserveRequest();
        long documentId = adapter.reserveDocumentId();
        String key = ExamObjectKey.pdf(PROGRAM_ID, STUDENT_ID, requestId, documentId);
        StoredDocumentReference tesis = pdf(documentId, requestId, DocumentType.TESIS, key, TESIS_NAME, SHA_A, 1200L);
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.persist(ExamRequestEntity.stored(
                    requestId,
                    STUDENT_ID,
                    ExamType.GRADO,
                    PROGRAM_ID,
                    Degree.MAESTRIA,
                    ExamRequestStatus.ENVIADA,
                    "SEG-000010-000001",
                    4,
                    LATER));
            entityManager.persist(ExamDocumentEntity.from(tesis));
        });
        StoredDocumentReference replacement = putPdf(STUDENT_ID, PROGRAM_ID, requestId, DocumentType.TESIS, TESIS_NAME, SHA_B);

        assertThatThrownBy(() -> adapter.delete(new Deletion(STUDENT_ID, ExamType.GRADO, 4, DocumentType.TESIS, LATER)))
                .isInstanceOf(ExamRequestNotDraftException.class);
        assertThatThrownBy(() -> adapter.save(new Save(STUDENT_ID, ExamType.GRADO, 4, List.of(replacement))))
                .isInstanceOf(ExamRequestNotDraftException.class);

        ExamRequestEntity stored = requests.findByStudentIdAndExamType(STUDENT_ID, ExamType.GRADO).orElseThrow();
        assertThat(stored.getVersion()).isEqualTo(4);
        assertThat(stored.getStatus()).isEqualTo(ExamRequestStatus.ENVIADA);
        assertThat(documentRows.findAll()).singleElement().extracting(ExamDocumentEntity::getId).isEqualTo(documentId);
        assertThat(marks.count()).isZero();
    }

    @Test
    @DisplayName("save and delete do not create a draft")
    void saveAndDeleteDoNotCreateADraft() {
        assertThatThrownBy(() -> adapter.save(new Save(
                STUDENT_ID, ExamType.GRADO, 1, List.of(detachedPdf(DocumentType.TESIS)))))
                .isInstanceOf(ExamDraftNotFoundException.class);
        assertThatThrownBy(() -> adapter.delete(new Deletion(STUDENT_ID, ExamType.GRADO, 1, DocumentType.TESIS, LATER)))
                .isInstanceOf(ExamDraftNotFoundException.class);
        assertThat(requests.count()).isZero();
    }

    @Test
    @DisplayName("deleting a type that is not stored leaves the version unchanged")
    void deletingAMissingTypeLeavesTheVersion() {
        StoredDocumentReference sol = putPdf(
                STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.SOL_SINODALES, SOL_NAME, SHA_A);
        adapter.create(grado(sol));

        assertThatThrownBy(() -> adapter.delete(new Deletion(STUDENT_ID, ExamType.GRADO, 1, DocumentType.TESIS, LATER)))
                .isInstanceOf(ExamDocumentNotFoundException.class);

        ExamRequestRead current = adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO).orElseThrow();
        assertThat(current.binding().version()).isEqualTo(1);
        assertThat(current.documents()).extracting(CurrentExamDocument::documentType)
                .containsExactly(DocumentType.SOL_SINODALES);
        assertThat(marks.count()).isZero();
    }

    @Test
    @DisplayName("two concurrent first saves leave a single draft")
    void twoFirstSavesLeaveASingleDraft() {
        long firstRequest = reserveRequest();
        long secondRequest = reserveRequest();
        StoredDocumentReference first = putPdf(STUDENT_ID, PROGRAM_ID, firstRequest, DocumentType.TESIS, TESIS_NAME, SHA_A);
        StoredDocumentReference second = putPdf(STUDENT_ID, PROGRAM_ID, secondRequest, DocumentType.TESIS, TESIS_NAME, SHA_B);
        adapter.create(grado(first));

        assertThatThrownBy(() -> adapter.create(grado(second)))
                .isInstanceOf(ExamDraftConflictException.class);

        assertThat(requests.count()).isEqualTo(1);
        assertThat(requests.findAll()).singleElement().satisfies(row -> {
            assertThat(row.getId()).isEqualTo(firstRequest);
            assertThat(row.getGraduateProgramId()).isEqualTo(PROGRAM_ID);
        });
        assertThat(documentRows.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("a changed program does not create a second request")
    void changedProgramDoesNotCreateASecondRequest() {
        StoredDocumentReference first = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        adapter.create(grado(first));
        long otherRequest = reserveRequest();
        StoredDocumentReference second = putPdf(
                STUDENT_ID, OTHER_PROGRAM_ID, otherRequest, DocumentType.TESIS, TESIS_NAME, SHA_B);

        assertThatThrownBy(() -> adapter.create(new Creation(
                otherRequest, STUDENT_ID, ExamType.GRADO, OTHER_PROGRAM_ID, Degree.DOCTORADO, List.of(second))))
                .isInstanceOf(ExamDraftConflictException.class);

        assertThat(requests.count()).isEqualTo(1);
        assertThat(requests.findAll().get(0).getGraduateProgramId()).isEqualTo(PROGRAM_ID);
        assertThat(requests.findAll().get(0).getDegree()).isEqualTo(Degree.MAESTRIA);
    }

    @Test
    @DisplayName("a stale version changes nothing")
    void staleVersionChangesNothing() {
        long requestId = reserveRequest();
        StoredDocumentReference tesis = putPdf(STUDENT_ID, PROGRAM_ID, requestId, DocumentType.TESIS, TESIS_NAME, SHA_A);
        adapter.create(grado(tesis));
        StoredDocumentReference sol = putPdf(STUDENT_ID, PROGRAM_ID, requestId, DocumentType.SOL_SINODALES, SOL_NAME, SHA_B);
        adapter.save(new Save(STUDENT_ID, ExamType.GRADO, 1, List.of(sol)));
        StoredDocumentReference replacement = putPdf(STUDENT_ID, PROGRAM_ID, requestId, DocumentType.TESIS, TESIS_NAME, SHA_A);

        StaleExamDraftException stale = catchThrowableOfType(
                StaleExamDraftException.class,
                () -> adapter.save(new Save(STUDENT_ID, ExamType.GRADO, 1, List.of(replacement))));

        assertThat(stale.storedVersion()).isEqualTo(2);
        ExamRequestRead current = adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO).orElseThrow();
        assertThat(current.binding().version()).isEqualTo(2);
        assertThat(current.documents()).extracting(CurrentExamDocument::id).containsExactly(tesis.id(), sol.id());
        assertThat(marks.count()).isZero();
    }

    @Test
    @DisplayName("the same student may hold GRADO and PREDOCTORAL")
    void oneStudentMayHoldBothExamTypes() {
        StoredDocumentReference grado = putPdf(STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_A);
        StoredDocumentReference predoctoral = putPdf(
                STUDENT_ID, PROGRAM_ID, reserveRequest(), DocumentType.TESIS, TESIS_NAME, SHA_B);
        adapter.create(grado(grado));
        adapter.create(new Creation(
                predoctoral.examRequestId(),
                STUDENT_ID,
                ExamType.PREDOCTORAL,
                PROGRAM_ID,
                Degree.DOCTORADO,
                List.of(predoctoral)));

        assertThat(requests.count()).isEqualTo(2);
        assertThat(adapter.findByStudentAndType(STUDENT_ID, ExamType.GRADO)).isPresent();
        assertThat(adapter.findByStudentAndType(STUDENT_ID, ExamType.PREDOCTORAL)).isPresent();
    }

    private long reserveRequest() {
        return adapter.reserveRequestId();
    }

    private Creation grado(StoredDocumentReference first, StoredDocumentReference... rest) {
        List<StoredDocumentReference> documents = new ArrayList<>();
        documents.add(first);
        documents.addAll(List.of(rest));
        return new Creation(first.examRequestId(), STUDENT_ID, ExamType.GRADO, PROGRAM_ID, Degree.MAESTRIA, documents);
    }

    private StoredDocumentReference putPdf(
            long studentId,
            long programId,
            long requestId,
            DocumentType type,
            String name,
            String sha) {
        long documentId = adapter.reserveDocumentId();
        String key = ExamObjectKey.pdf(programId, studentId, requestId, documentId);
        StoredDocumentReference reference = pdf(documentId, requestId, type, key, name, sha, 1200L);
        storage.put(new PrivateObjectPut(key, sha, StoredDocumentReference.PDF_MEDIA_TYPE, 1200L), InputStream.nullInputStream());
        return reference;
    }

    private StoredDocumentReference detachedPdf(DocumentType type) {
        return pdf(1L, 1L, type, ExamObjectKey.pdf(PROGRAM_ID, STUDENT_ID, 1L, 1L), TESIS_NAME, SHA_A, 1200L);
    }

    private static StoredDocumentReference pdf(
            long documentId,
            long requestId,
            DocumentType type,
            String key,
            String name,
            String sha,
            long sizeBytes) {
        return new StoredDocumentReference(
                documentId,
                requestId,
                type,
                DocumentKind.PDF,
                key,
                sha,
                StoredDocumentReference.PDF_MEDIA_TYPE,
                name,
                null,
                sizeBytes,
                UPLOADED_AT);
    }

    private void rejectMarks() {
        execute("""
                ALTER TABLE exam_request_superseded_objects
                ADD CONSTRAINT ck_exam_superseded_fail CHECK (sha256 = 'never')
                """);
    }

    private void dropMarkRejection() {
        execute("ALTER TABLE exam_request_superseded_objects DROP CONSTRAINT IF EXISTS ck_exam_superseded_fail");
    }

    private void reset() {
        dropMarkRejection();
        transactionTemplate.executeWithoutResult(status -> {
            marks.deleteAll();
            documentRows.deleteAll();
            requests.deleteAll();
        });
        execute("CREATE SEQUENCE IF NOT EXISTS exam_requests_id_seq START WITH 1 INCREMENT BY 1");
        execute("CREATE SEQUENCE IF NOT EXISTS exam_request_documents_id_seq START WITH 1 INCREMENT BY 1");
    }

    private void execute(String sql) {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException(sql, exception);
        }
    }

    /**
     * Double of {@link DocumentStoragePort}. Records puts and uncommitted notices. Does not insert a mark.
     */
    private static final class RecordingStorage implements DocumentStoragePort {

        private final List<PrivateObjectPut> puts = new ArrayList<>();
        private final List<UncommittedObjectNotice> notices = new ArrayList<>();
        private final Set<String> failingKeys = new HashSet<>();

        @Override
        public StoredObjectReceipt put(PrivateObjectPut command, InputStream body) {
            if (failingKeys.contains(command.objectKey())) {
                throw new ObjectStorageException("Object write failed");
            }
            puts.add(command);
            return new StoredObjectReceipt(
                    command.objectKey(), command.sha256(), command.contentType(), command.sizeBytes());
        }

        @Override
        public UncommittedObjectNotice noticeUncommitted(String objectKey, String sha256) {
            UncommittedObjectNotice notice = new UncommittedObjectNotice(objectKey, sha256);
            notices.add(notice);
            return notice;
        }

        private void fail(String objectKey) {
            failingKeys.add(objectKey);
        }
    }
}

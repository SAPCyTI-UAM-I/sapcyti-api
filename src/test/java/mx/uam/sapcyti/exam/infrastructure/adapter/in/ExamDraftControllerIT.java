package mx.uam.sapcyti.exam.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import mx.uam.sapcyti.academic.domain.model.AcademicInformation;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.StudentExamContextPort;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataStudentProgramRepository;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataStudentRepository;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.exam.domain.port.out.DocumentStoragePort;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestDraftPort;
import mx.uam.sapcyti.exam.infrastructure.adapter.out.S3ObjectClient;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * SPEC-052 contract for draft save, replace, delete and the panel.
 * SPEC-051 supplies the rows. The S3 client is a double, not a bucket.
 * Role checks use {@code SecurityConfig} ({@code integration-test}), not {@code TestSecurityConfig}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class ExamDraftControllerIT {

    private static final String PASSWORD = "password";
    private static final String TESIS = "Tesis_Mtria_Lopez_Alba_Emanuel.pdf";
    private static final String SOL = "Sol_Sinodales_Lopez_Alba_Emanuel.pdf";
    private static final String HIST = "Hist_Acad_Lopez_Alba_Emanuel.pdf";
    private static final String ARTICLE = "https://revista.ejemplo.org/articulo/123";
    private static final int TWENTY_MB = 20 * 1024 * 1024;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataStudentRepository studentRepository;

    @Autowired
    private SpringDataStudentProgramRepository studentProgramRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    @MockitoBean
    private S3ObjectClient objects;

    @MockitoSpyBean
    private DocumentStoragePort storage;

    @MockitoSpyBean
    private ExamRequestDraftPort drafts;

    @MockitoSpyBean
    private StudentExamContextPort studentExamContextPort;

    @BeforeEach
    void sequences() {
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS exam_requests_id_seq START WITH 1 INCREMENT BY 1");
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS exam_request_documents_id_seq START WITH 1 INCREMENT BY 1");
        doCallRealMethod().when(drafts).save(any());
        doCallRealMethod().when(drafts).create(any());
    }

    @Test
    @DisplayName("Scenario: A valid selection creates the draft and the panel shows the stored documents")
    void validSelectionCreatesTheDraft() throws Exception {
        Enrolled student = emanuel();
        int rows = requestRows();

        MvcResult saved = postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf())
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().exists(TenantFilter.HEADER_REQUEST_ID))
                .andExpect(jsonPath("$.results[0].documentType").value("TESIS"))
                .andExpect(jsonPath("$.results[0].status").value("SAVED"))
                .andExpect(jsonPath("$.request.status").value("BORRADOR"))
                .andExpect(jsonPath("$.request.version").value(1))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();
        assertThat(saved.getResponse().getContentAsString()).doesNotContain("objectKey", "bucket", "sha256");
        assertThat(requestRows()).isEqualTo(rows + 1);

        mockMvc.perform(get("/api/exam-requests/me/GRADO")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.requirements[0].documentType").value("TESIS"))
                .andExpect(jsonPath("$.requirements[0].status").value("CARGADO"))
                .andExpect(jsonPath("$.requirements[0].currentDocument.originalName").value(TESIS))
                .andExpect(jsonPath("$.requirements[0].currentDocument.sizeBytes").value(greaterThan(0)))
                .andExpect(jsonPath("$.requirements[0].currentDocument.uploadedAt").exists())
                .andExpect(jsonPath("$.requiredPendingCount").value(4));
    }

    @Test
    @DisplayName("Scenario: Saving does not submit the request")
    void savingDoesNotSubmit() throws Exception {
        Enrolled student = emanuel();

        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.status").value("BORRADOR"))
                .andExpect(jsonPath("$.request.folio").value(nullValue()))
                .andExpect(jsonPath("$.request.submittedAt").value(nullValue()))
                .andExpect(jsonPath("$.request.delivery").value(nullValue()));

        Integer folios = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exam_requests WHERE student_id = ? AND folio IS NOT NULL",
                Integer.class,
                student.studentId());
        assertThat(folios).isZero();
    }

    @Test
    @DisplayName("Scenario: A later visit shows the saved documents and the pending ones")
    void laterVisitShowsThreeOfFive() throws Exception {
        Enrolled student = emanuel();
        postBatch(student, "GRADO", 0, pdfPart("TESIS", TESIS), pdfPart("SOL_SINODALES", SOL), pdfPart("HIST_ACAD", HIST))
                .andExpect(status().isOk());
        int rows = requestRows();

        mockMvc.perform(get("/api/exam-requests/me/GRADO")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[?(@.status == 'CARGADO')]").value(org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.requirements[?(@.status == 'PENDIENTE')]").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.requiredPendingCount").value(2));

        assertThat(requestRows()).isEqualTo(rows);
    }

    @Test
    @DisplayName("Scenario: Several saved documents in one batch increment the version once")
    void oneBatchIncrementsTheVersionOnce() throws Exception {
        Enrolled student = emanuel();

        postBatch(student, "GRADO", 0, pdfPart("TESIS", TESIS), pdfPart("SOL_SINODALES", SOL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("SAVED"))
                .andExpect(jsonPath("$.results[1].status").value("SAVED"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.request.version").value(1));
    }

    @Test
    @DisplayName("Scenario: A PDF larger than 20 MB is stored")
    void pdfLargerThanTwentyMegabytesIsStored() throws Exception {
        Enrolled student = emanuel();

        postPdf(student, "GRADO", 0, "TESIS", TESIS, largePdf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("SAVED"))
                .andExpect(jsonPath("$.results[0].document.sizeBytes").value(greaterThan(TWENTY_MB)));
    }

    @Test
    @DisplayName("Scenario: An article URL is saved without an object")
    void articleUrlIsSavedWithoutAnObject() throws Exception {
        Enrolled student = enroll(ProgramType.DOCTORADO, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");
        clearInvocations(storage);

        String manifest = manifest(0, urlDescriptor("ARTICULO", ARTICLE));
        post(student, "GRADO", manifest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("SAVED"))
                .andExpect(jsonPath("$.results[0].document.kind").value("URL"))
                .andExpect(jsonPath("$.results[0].document.externalUrl").value(ARTICLE))
                .andExpect(jsonPath("$.results[0].document.originalName").value(nullValue()))
                .andExpect(jsonPath("$.results[0].document.sizeBytes").value(nullValue()));

        verify(storage, never()).put(any(), any());
        verify(objects, never()).putNew(any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("Scenario: One invalid document does not discard the documents that were stored")
    void oneInvalidDocumentKeepsTheStoredOne() throws Exception {
        Enrolled student = emanuel();

        postBatch(student, "GRADO", 0, pdfPart("TESIS", TESIS), pdfPart("HIST_ACAD", "historia.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("SAVED"))
                .andExpect(jsonPath("$.results[1].documentType").value("HIST_ACAD"))
                .andExpect(jsonPath("$.results[1].status").value("ERROR"))
                .andExpect(jsonPath("$.results[1].error.error").value("INVALID_DOCUMENT_NAME"))
                .andExpect(jsonPath("$.results[1].error.details.expectedName").value(HIST))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(get("/api/exam-requests/me/GRADO")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[?(@.documentType == 'TESIS')].status").value("CARGADO"))
                .andExpect(jsonPath("$.requirements[?(@.documentType == 'HIST_ACAD')].status").value("PENDIENTE"));
    }

    @Test
    @DisplayName("Scenario: A failed object write reports that document and keeps the others")
    void failedObjectWriteKeepsTheOthers() throws Exception {
        Enrolled student = emanuel();
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) {
                throw new RuntimeException("storage down");
            }
            return null;
        }).when(objects).putNew(any(), any(), any(), anyLong());

        postBatch(student, "GRADO", 0, pdfPart("TESIS", TESIS), pdfPart("HIST_ACAD", HIST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("SAVED"))
                .andExpect(jsonPath("$.results[1].status").value("ERROR"))
                .andExpect(jsonPath("$.results[1].error.error").value("STORAGE_WRITE_FAILED"))
                .andExpect(jsonPath("$.results[1].error.details").doesNotExist());

        Integer hist = jdbc.queryForObject("""
                SELECT COUNT(*) FROM exam_request_documents document
                JOIN exam_requests request ON request.id = document.exam_request_id
                WHERE request.student_id = ? AND document.document_type = 'HIST_ACAD'
                """, Integer.class, student.studentId());
        assertThat(hist).isZero();
    }

    @Test
    @DisplayName("Scenario: A batch in which every document fails creates no draft")
    void everyFailureCreatesNoDraft() throws Exception {
        Enrolled student = emanuel();
        int rows = requestRows();

        postPdf(student, "GRADO", 0, "HIST_ACAD", "historia.pdf", pdf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request").value(nullValue()))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.results[0].status").value("ERROR"));

        assertThat(requestRows()).isEqualTo(rows);
    }

    @Test
    @DisplayName("Scenario: A failed document does not replace the reference already stored")
    void failedDocumentKeepsThePreviousReference() throws Exception {
        Enrolled student = emanuel();
        long documentId = savedThesisId(student);

        postPdf(student, "GRADO", 1, "TESIS", "historia.pdf", pdf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("ERROR"))
                .andExpect(jsonPath("$.results[0].error.error").value("INVALID_DOCUMENT_NAME"))
                .andExpect(jsonPath("$.request.version").value(1));

        assertThat(currentThesisId(student.studentId())).isEqualTo(documentId);
    }

    @Test
    @DisplayName("Scenario: An invalid envelope changes nothing")
    void invalidEnvelopeChangesNothing() throws Exception {
        Enrolled student = emanuel();
        long documentId = savedThesisId(student);
        int rows = requestRows();

        post(student, "GRADO", "{\"version\":1,\"documents\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.results").doesNotExist());

        assertThat(requestRows()).isEqualTo(rows);
        assertThat(versionOf(student.studentId())).isEqualTo(1);
        assertThat(currentThesisId(student.studentId())).isEqualTo(documentId);
    }

    @Test
    @DisplayName("Scenario: Nothing useful to save does not create a draft")
    void nothingUsefulDoesNotCreateADraft() throws Exception {
        Enrolled student = emanuel();
        int rows = requestRows();

        mockMvc.perform(multipart("/api/exam-requests/me/GRADO/documents")
                        .file(new MockMultipartFile("note", "note.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.results").doesNotExist());

        assertThat(requestRows()).isEqualTo(rows);
    }

    @Test
    @DisplayName("Scenario: A database commit failure publishes no saved result")
    void commitFailurePublishesNoSavedResult() throws Exception {
        Enrolled student = emanuel();
        long documentId = savedThesisId(student);
        int marks = markRows();
        doThrow(new DataAccessResourceFailureException("commit")).when(drafts).save(any());
        clearInvocations(storage);

        try {
            MvcResult failed = postPdf(student, "GRADO", 1, "TESIS", TESIS, pdf())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("DEPENDENCY_UNAVAILABLE"))
                    .andExpect(jsonPath("$.results").doesNotExist())
                    .andReturn();
            assertThat(failed.getResponse().getContentAsString()).doesNotContain("SAVED");
        } finally {
            doCallRealMethod().when(drafts).save(any());
        }

        assertThat(currentThesisId(student.studentId())).isEqualTo(documentId);
        assertThat(markRows()).isEqualTo(marks);
        verify(storage).noticeUncommitted(any(), any());
    }

    @Test
    @DisplayName("Scenario: Deleting a draft document makes that type pending again")
    void deletingMakesTheTypePending() throws Exception {
        Enrolled student = emanuel();
        savedThesisId(student);
        int marks = markRows();

        deleteThesis(student, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value(nullValue()))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.request.requirements[?(@.documentType == 'TESIS')].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.request.folio").value(nullValue()))
                .andExpect(jsonPath("$.request.delivery").value(nullValue()));

        assertThat(markRows()).isEqualTo(marks + 1);
        assertThat(thesisRows(student.studentId())).isZero();
        panel(student)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[0].documentType").value("TESIS"))
                .andExpect(jsonPath("$.requirements[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.requirements[0].currentDocument").value(nullValue()));
    }

    @Test
    @DisplayName("Scenario: Replacing a draft document leaves only the new reference visible")
    void replacingLeavesOnlyTheNewReference() throws Exception {
        Enrolled student = emanuel();
        long previous = savedThesisId(student);
        int marks = markRows();

        putThesis(student, 1, TESIS, pdf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.document.id").value(org.hamcrest.Matchers.not(previous)))
                .andExpect(jsonPath("$.request.folio").value(nullValue()));

        assertThat(markRows()).isEqualTo(marks + 1);
        assertThat(thesisRows(student.studentId())).isEqualTo(1);
        assertThat(currentThesisId(student.studentId())).isNotEqualTo(previous);
    }

    @Test
    @DisplayName("Scenario: A rejected replacement keeps the previous reference")
    void rejectedReplacementKeepsThePreviousReference() throws Exception {
        Enrolled student = emanuel();
        long previous = savedThesisId(student);
        int marks = markRows();

        putThesis(student, 1, "historia.pdf", pdf())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DOCUMENT_NAME"));

        assertThat(versionOf(student.studentId())).isEqualTo(1);
        assertThat(currentThesisId(student.studentId())).isEqualTo(previous);
        assertThat(markRows()).isEqualTo(marks);
    }

    @Test
    @DisplayName("Scenario: Replacing or deleting a sent request is rejected")
    void sentRequestRejectsReplaceAndDelete() throws Exception {
        Enrolled student = emanuel();
        long documentId = insertSentThesis(student);
        int marks = markRows();

        deleteThesis(student, 4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REQUEST_NOT_DRAFT"));
        putThesis(student, 4, TESIS, pdf())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REQUEST_NOT_DRAFT"));

        assertThat(versionOf(student.studentId())).isEqualTo(4);
        assertThat(currentThesisId(student.studentId())).isEqualTo(documentId);
        assertThat(markRows()).isEqualTo(marks);
    }

    @Test
    @DisplayName("Scenario: Replace and delete do not create a draft")
    void replaceAndDeleteDoNotCreateADraft() throws Exception {
        Enrolled student = emanuel();
        int rows = requestRows();

        putThesis(student, 1, TESIS, pdf())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REQUEST_NOT_FOUND"));
        assertThat(requestRows()).isEqualTo(rows);

        deleteThesis(student, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REQUEST_NOT_FOUND"));
        assertThat(requestRows()).isEqualTo(rows);
    }

    @Test
    @DisplayName("Scenario: Deleting a type that is not stored returns not found")
    void deletingAMissingTypeReturnsNotFound() throws Exception {
        Enrolled student = emanuel();
        postPdf(student, "GRADO", 0, "SOL_SINODALES", SOL, pdf()).andExpect(status().isOk());

        deleteThesis(student, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_FOUND"));

        assertThat(versionOf(student.studentId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario: Opening the panel without a draft does not create one")
    void panelWithoutADraftDoesNotCreateOne() throws Exception {
        Enrolled student = emanuel();
        int rows = requestRows();

        panel(student)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.request").value(nullValue()))
                .andExpect(jsonPath("$.requirements.length()").value(5))
                .andExpect(jsonPath("$.requirements[?(@.status == 'PENDIENTE')]")
                        .value(org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$.requiredPendingCount").value(5));

        assertThat(requestRows()).isEqualTo(rows);
    }

    @Test
    @DisplayName("Scenario: Two concurrent first saves leave a single draft")
    void twoFirstSavesLeaveASingleDraft() throws Exception {
        Enrolled student = emanuel();
        int rows = requestRows();

        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf()).andExpect(status().isOk());
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("VERSION_CONFLICT"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("version"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("STALE_VERSION"));

        assertThat(requestRows()).isEqualTo(rows + 1);
    }

    @Test
    @DisplayName("Scenario: A stale version changes nothing")
    void staleVersionChangesNothing() throws Exception {
        Enrolled student = emanuel();
        savedThesisId(student);
        putThesis(student, 1, TESIS, pdf()).andExpect(status().isOk());

        postPdf(student, "GRADO", 1, "TESIS", TESIS, pdf())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("VERSION_CONFLICT"));

        assertThat(versionOf(student.studentId())).isEqualTo(2);
    }

    @Test
    @DisplayName("Scenario: A student on leave cannot save")
    void studentOnLeaveCannotSave() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.BAJA, "Emanuel", "López", "Alba");
        int rows = requestRows();

        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PROGRAM_NOT_ELIGIBLE"));
        assertThat(requestRows()).isEqualTo(rows);

        panel(student)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.eligibilityReason").value("PROGRAM_BAJA"));
    }

    @Test
    @DisplayName("Scenario: A master's student cannot save a predoctoral draft")
    void masterCannotSavePredoctoral() throws Exception {
        Enrolled student = emanuel();
        int rows = requestRows();
        clearInvocations(storage);

        postPdf(student, "PREDOCTORAL", 0, "TESIS", TESIS, pdf())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("EXAM_TYPE_NOT_ALLOWED"));

        assertThat(requestRows()).isEqualTo(rows);
        verify(storage, never()).put(any(), any());
    }

    @Test
    @DisplayName("Scenario: A changed program does not create a second request")
    void changedProgramDoesNotCreateASecondRequest() throws Exception {
        Enrolled student = emanuel();
        savedThesisId(student);
        int rows = requestRows();
        jdbc.update("UPDATE exam_requests SET graduate_program_id = graduate_program_id + 1 WHERE student_id = ?",
                student.studentId());

        postPdf(student, "GRADO", 1, "TESIS", TESIS, pdf())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ACADEMIC_PROGRAM_CHANGED"));

        assertThat(requestRows()).isEqualTo(rows);
    }

    @Test
    @DisplayName("Scenario: A caller without the student role is rejected before the body is validated")
    void nonStudentIsRejectedBeforeTheBody() throws Exception {
        GraduateProgram program = programAdapter.save(new GraduateProgram("Draft coord " + System.nanoTime(), "CBI"));
        String token = login(saveUser(RoleType.COORDINATOR, program.getId()));
        int rows = requestRows();
        clearInvocations(studentExamContextPort);

        mockMvc.perform(multipart("/api/exam-requests/me/GRADO/documents")
                        .file(new MockMultipartFile("manifest", "manifest.json", "text/plain", "{".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, program.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        verify(studentExamContextPort, never()).resolve(any(), any());
        assertThat(requestRows()).isEqualTo(rows);
    }

    @Test
    @DisplayName("Scenario: A graduate header outside the caller scope is rejected")
    void graduateHeaderOutsideScopeIsRejected() throws Exception {
        Enrolled student = emanuel();
        Enrolled other = enroll(ProgramType.DOCTORADO, ProgramStatus.EN_INVESTIGACION, "Secreto", "Ajeno", "Cruz");
        int rows = requestRows();

        MvcResult result = mockMvc.perform(get("/api/exam-requests/me/GRADO")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, 99))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("Secreto");
        assertThat(other.studentId()).isNotNull();
        assertThat(requestRows()).isEqualTo(rows);
    }

    private long savedThesisId(Enrolled student) throws Exception {
        MvcResult result = postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf())
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("results").get(0).get("document").get("id").asLong();
    }

    private ResultActions postPdf(
            Enrolled student, String examType, int version, String type, String filename, byte[] bytes)
            throws Exception {
        return postBatch(student, examType, version, pdfPart(type, filename, bytes));
    }

    private ResultActions postBatch(Enrolled student, String examType, int version, MockMultipartFile... files)
            throws Exception {
        String[] descriptors = new String[files.length];
        for (int index = 0; index < files.length; index++) {
            String type = files[index].getName().substring("file_".length());
            descriptors[index] = pdfDescriptor(type);
        }
        return post(student, examType, manifest(version, descriptors), files);
    }

    private ResultActions post(Enrolled student, String examType, String manifest, MockMultipartFile... files)
            throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/exam-requests/me/" + examType + "/documents")
                .file(new MockMultipartFile(
                        "manifest", "manifest.json", "application/json", manifest.getBytes(StandardCharsets.UTF_8)));
        for (MockMultipartFile file : files) {
            builder.file(file);
        }
        return mockMvc.perform(builder
                .header("Authorization", "Bearer " + student.token())
                .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()));
    }

    private ResultActions putThesis(Enrolled student, int version, String filename, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/exam-requests/me/GRADO/documents/TESIS")
                .file(new MockMultipartFile(
                        "version", "", "text/plain", Integer.toString(version).getBytes(StandardCharsets.US_ASCII)))
                .file(new MockMultipartFile("file", filename, "application/pdf", bytes))
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .header("Authorization", "Bearer " + student.token())
                .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()));
    }

    private ResultActions deleteThesis(Enrolled student, int version) throws Exception {
        return mockMvc.perform(delete("/api/exam-requests/me/GRADO/documents/TESIS")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + version + "}")
                .header("Authorization", "Bearer " + student.token())
                .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()));
    }

    private ResultActions panel(Enrolled student) throws Exception {
        return mockMvc.perform(get("/api/exam-requests/me/GRADO")
                .header("Authorization", "Bearer " + student.token())
                .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()));
    }

    private static MockMultipartFile pdfPart(String type, String filename) throws IOException {
        return pdfPart(type, filename, pdf());
    }

    private static MockMultipartFile pdfPart(String type, String filename, byte[] bytes) {
        return new MockMultipartFile("file_" + type, filename, "application/pdf", bytes);
    }

    private static String manifest(int version, String... documents) {
        return "{\"version\":" + version + ",\"documents\":[" + String.join(",", documents) + "]}";
    }

    private static String pdfDescriptor(String type) {
        return "{\"documentType\":\"" + type + "\",\"kind\":\"PDF\",\"filePart\":\"file_" + type + "\"}";
    }

    private static String urlDescriptor(String type, String url) {
        return "{\"documentType\":\"" + type + "\",\"kind\":\"URL\",\"externalUrl\":\"" + url + "\"}";
    }

    private static byte[] pdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] largePdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            var stream = document.getDocument().createCOSStream();
            try (OutputStream raw = stream.createOutputStream()) {
                raw.write(new byte[TWENTY_MB + 1]);
            }
            document.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("Pad"), stream);
            document.save(out);
            return out.toByteArray();
        }
    }

    private long insertSentThesis(Enrolled student) throws IOException {
        long requestId = 1_000_000_000L + (System.nanoTime() % 1_000_000L);
        long documentId = requestId + 1;
        String folio = String.format("SEG-%06d-%06d", requestId % 1_000_000, documentId % 1_000_000);
        jdbc.update("""
                INSERT INTO exam_requests (
                    id, student_id, exam_type, graduate_program_id, degree, status, folio, version, submitted_at)
                VALUES (?, ?, 'GRADO', ?, 'MAESTRIA', 'ENVIADA', ?, 4, TIMESTAMP '2026-10-12 18:00:00')
                """, requestId, student.studentId(), student.programId(), folio);
        jdbc.update("""
                INSERT INTO exam_request_documents (
                    id, exam_request_id, document_type, kind, object_key, sha256, content_type,
                    original_name, external_url, size_bytes, uploaded_at)
                VALUES (?, ?, 'TESIS', 'PDF', ?, ?, 'application/pdf', ?, NULL, 1200,
                    TIMESTAMP '2026-10-12 18:00:00')
                """, documentId, requestId, "17/" + student.studentId() + "/exam-requests/"
                + requestId + "/" + documentId + ".pdf", "a".repeat(64), TESIS);
        return documentId;
    }

    private int requestRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exam_requests", Integer.class);
    }

    private int markRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exam_request_superseded_objects", Integer.class);
    }

    private int versionOf(long studentId) {
        return jdbc.queryForObject(
                "SELECT version FROM exam_requests WHERE student_id = ?", Integer.class, studentId);
    }

    private int thesisRows(long studentId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM exam_request_documents document
                JOIN exam_requests request ON request.id = document.exam_request_id
                WHERE request.student_id = ? AND document.document_type = 'TESIS'
                """, Integer.class, studentId);
    }

    private long currentThesisId(long studentId) {
        return jdbc.queryForObject("""
                SELECT document.id FROM exam_request_documents document
                JOIN exam_requests request ON request.id = document.exam_request_id
                WHERE request.student_id = ? AND document.document_type = 'TESIS'
                """, Long.class, studentId);
    }

    private Enrolled emanuel() {
        return enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");
    }

    private Enrolled enroll(
            ProgramType programType, ProgramStatus status, String firstName, String paternal, String maternal) {
        GraduateProgram program = programAdapter.save(
                new GraduateProgram("Draft " + programType + " " + System.nanoTime(), "CBI"));
        User user = saveUser(RoleType.STUDENT, program.getId());
        String enrollmentId = Long.toUnsignedString(System.nanoTime());
        Student student = studentRepository.save(new Student(
                enrollmentId,
                user.getId(),
                program.getId(),
                null,
                new PersonalData(firstName, paternal, maternal, "MX", LocalDate.of(1998, 3, 15), "5555555555", null),
                new AcademicInformation(
                        "Licenciatura",
                        DegreeLevel.LICENCIATURA,
                        programType,
                        LocalDate.of(2024, 1, 15),
                        "24I")));
        StudentProgram academicProgram = studentProgramRepository.save(StudentProgram.forRegistration(
                student.getId(), program.getId(), enrollmentId, programType, LocalDate.of(2024, 1, 15), null));
        if (status != ProgramStatus.EN_INVESTIGACION) {
            String reason = status == ProgramStatus.BAJA || status == ProgramStatus.SUSPENSION ? "motivo" : null;
            academicProgram.updateMetadata(
                    academicProgram.getAdmissionDate(), null, null, null, status, reason, null);
            studentProgramRepository.save(academicProgram);
        }
        return new Enrolled(program.getId(), student.getId(), login(user));
    }

    private User saveUser(RoleType role, Long programId) {
        String email = "draft-" + role.name().toLowerCase() + "-" + System.nanoTime() + "@uam.mx";
        return userRepository.save(new User(
                email, new BCryptPasswordEncoder().encode(PASSWORD), role, programId));
    }

    private String login(User user) {
        try {
            LoginRequest request = LoginRequest.builder().email(user.getEmail()).password(PASSWORD).build();
            MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                            "/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();
            return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        } catch (Exception ex) {
            throw new IllegalStateException("login failed", ex);
        }
    }

    private record Enrolled(Long programId, Long studentId, String token) {
    }
}

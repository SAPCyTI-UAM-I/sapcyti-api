package mx.uam.sapcyti.exam.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import mx.uam.sapcyti.exam.domain.exception.ObjectStorageException;
import mx.uam.sapcyti.exam.domain.model.OpenedObject;
import mx.uam.sapcyti.exam.domain.port.out.DocumentStoragePort;
import mx.uam.sapcyti.exam.infrastructure.adapter.out.S3ObjectClient;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * SPEC-053 contract for the current PDF. The S3 client is a double.
 * Role checks use {@code SecurityConfig} ({@code integration-test}), not {@code TestSecurityConfig}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class ExamDocumentContentIT {

    private static final String PASSWORD = "password";
    private static final String TESIS = "Tesis_Mtria_Lopez_Alba_Emanuel.pdf";
    private static final String DOCTORAL_TESIS = "Tesis_Docto_Paz_Sol_Ana.pdf";
    private static final String ARTICLE = "https://example.org/article";
    private static final String UNICODE_NAME = "Artículo de Peña y Núñez.pdf";

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
    private StudentExamContextPort studentExamContextPort;

    private final Map<String, byte[]> stored = new ConcurrentHashMap<>();

    @BeforeEach
    void storageDouble() throws IOException {
        stored.clear();
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS exam_requests_id_seq START WITH 1 INCREMENT BY 1");
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS exam_request_documents_id_seq START WITH 1 INCREMENT BY 1");
        doAnswer(invocation -> {
            InputStream body = invocation.getArgument(2);
            long size = invocation.getArgument(3);
            byte[] bytes = body.readAllBytes();
            if (bytes.length != size) {
                throw new IllegalStateException("incomplete");
            }
            stored.put(invocation.getArgument(0), bytes);
            return null;
        }).when(objects).putNew(any(), any(), any(), anyLong());
        doAnswer(invocation -> openStored(invocation.getArgument(0))).when(objects).open(any());
    }

    @Test
    @DisplayName("Scenario: Opening the student's own thesis returns the complete PDF")
    void ownThesisReturnsTheCompletePdf() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());
        String key = objectKey(student, "GRADO", "TESIS");

        MvcResult result = content(student, "GRADO", "TESIS", null)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Accept-Ranges", "none"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().exists(TenantFilter.HEADER_REQUEST_ID))
                .andExpect(header().doesNotExist("Content-Range"))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
        assertThat(result.getResponse().getContentLength()).isEqualTo(pdf.length);
        assertThat(result.getResponse().getHeader("Content-Disposition")).startsWith("inline;");
        assertThat(headerValues(result)).noneMatch(value -> value != null && value.contains(key));
        assertThat(headerValues(result)).noneMatch(value -> value != null && value.contains("bucket"));
    }

    @Test
    @DisplayName("Scenario: Downloading returns the same file that was uploaded")
    void downloadMatchesTheStoredHash() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());

        MvcResult result = content(student, "GRADO", "TESIS", "attachment")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
        assertThat(sha256(result.getResponse().getContentAsByteArray()))
                .isEqualTo(storedHash(student, "GRADO", "TESIS"));
    }

    @Test
    @DisplayName("Scenario: Inline and attachment change only the disposition")
    void inlineAndAttachmentChangeOnlyTheDisposition() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());

        MvcResult inline = content(student, "GRADO", "TESIS", "inline").andExpect(status().isOk()).andReturn();
        MvcResult attachment = content(student, "GRADO", "TESIS", "attachment").andExpect(status().isOk()).andReturn();

        assertThat(attachment.getResponse().getContentAsByteArray())
                .isEqualTo(inline.getResponse().getContentAsByteArray());
        String inlineHeader = inline.getResponse().getHeader("Content-Disposition");
        String attachmentHeader = attachment.getResponse().getHeader("Content-Disposition");
        assertThat(inlineHeader).startsWith("inline;");
        assertThat(attachmentHeader).startsWith("attachment;");
        assertThat(namesOf(attachmentHeader)).isEqualTo(namesOf(inlineHeader));
    }

    @Test
    @DisplayName("Scenario: A Unicode name is kept in the UTF-8 filename parameter")
    void unicodeNameIsKeptInFilenameStar() throws Exception {
        Enrolled student = doctoral();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "ARTICULO", UNICODE_NAME, pdf).andExpect(status().isOk());

        MvcResult result = content(student, "GRADO", "ARTICULO", null).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getHeader("Content-Disposition")).isEqualTo(
                "inline; filename=\"Articulo de Pena y Nunez.pdf\"; "
                        + "filename*=UTF-8''Art%C3%ADculo%20de%20Pe%C3%B1a%20y%20N%C3%BA%C3%B1ez.pdf");
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    @Test
    @DisplayName("Scenario: A hostile stored name cannot inject a header")
    void hostileStoredNameCannotInjectAHeader() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());
        jdbc.update("""
                UPDATE exam_request_documents
                SET original_name = ?
                WHERE exam_request_id = (
                    SELECT id FROM exam_requests WHERE student_id = ? AND exam_type = 'GRADO')
                """, "Tesis\"\r\nX-Evil: 1/../../secret.pdf", student.studentId());

        MvcResult result = content(student, "GRADO", "TESIS", null).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getHeaders("Content-Disposition")).hasSize(1);
        String disposition = result.getResponse().getHeader("Content-Disposition");
        assertThat(disposition).doesNotContain("\r").doesNotContain("\n");
        assertThat(disposition).startsWith("inline;");
        assertThat(result.getResponse().getHeader("X-Evil")).isNull();
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    @Test
    @DisplayName("Scenario: A document that belongs to another student is not found")
    void anotherStudentsDocumentIsNotFound() throws Exception {
        Enrolled owner = emanuel();
        Enrolled caller = enroll(ProgramType.MAESTRIA, "Nuria", "Sosa", "Leal");
        postPdf(owner, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());
        clearInvocations(storage);

        MvcResult result = content(caller, "GRADO", "TESIS", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Document not found"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(owner.studentId().toString());
        assertThat(result.getResponse().getContentAsString()).doesNotContain("exam-requests");
        verify(storage, never()).open(any());
    }

    @Test
    @DisplayName("Scenario: A missing current document uses the same not-found body")
    void missingDocumentUsesTheSameNotFoundBody() throws Exception {
        Enrolled owner = emanuel();
        Enrolled caller = enroll(ProgramType.MAESTRIA, "Nuria", "Sosa", "Leal");
        postPdf(owner, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());
        postPdf(caller, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());

        MvcResult foreign = content(enroll(ProgramType.MAESTRIA, "Olga", "Rios", "Paz"), "GRADO", "TESIS", null)
                .andExpect(status().isNotFound())
                .andReturn();
        clearInvocations(storage);
        MvcResult missing = content(caller, "GRADO", "SOL_SINODALES", null)
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(missing.getResponse().getStatus()).isEqualTo(foreign.getResponse().getStatus());
        assertThat(missing.getResponse().getContentAsString()).isEqualTo(foreign.getResponse().getContentAsString());
        assertThat(missing.getResponse().getContentAsString()).doesNotContain("details");
        verify(storage, never()).open(any());
    }

    @Test
    @DisplayName("Scenario: A previous document id is not a route")
    void previousDocumentIdIsNotARoute() throws Exception {
        Enrolled student = emanuel();
        byte[] first = severalPages();
        byte[] second = onePage();
        MvcResult saved = postPdf(student, "GRADO", 0, "TESIS", TESIS, first).andExpect(status().isOk()).andReturn();
        long previousId = objectMapper.readTree(saved.getResponse().getContentAsString())
                .get("results").get(0).get("document").get("id").asLong();
        putThesis(student, 1, TESIS, second).andExpect(status().isOk());

        MvcResult current = content(student, "GRADO", "TESIS", null).andExpect(status().isOk()).andReturn();
        assertThat(current.getResponse().getContentAsByteArray()).isEqualTo(second);

        clearInvocations(storage);
        MvcResult byId = content(student, "GRADO", Long.toString(previousId), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("documentType"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("INVALID_ENUM"))
                .andReturn();
        assertThat(byId.getResponse().getContentAsByteArray()).isNotEqualTo(first);
        verify(storage, never()).open(any());
    }

    @Test
    @DisplayName("Scenario: A caller without a session receives no PDF bytes")
    void callerWithoutASessionReceivesNoPdfBytes() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/exam-requests/me/GRADO/documents/TESIS/content"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEqualTo(pdf);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("%PDF");
    }

    @Test
    @DisplayName("Scenario: A caller without the student role is rejected before storage is opened")
    void nonStudentIsRejectedBeforeStorageIsOpened() throws Exception {
        GraduateProgram program = programAdapter.save(new GraduateProgram("Content coord " + System.nanoTime(), "CBI"));
        String token = login(saveUser(RoleType.COORDINATOR, program.getId()));
        clearInvocations(storage);

        mockMvc.perform(get("/api/exam-requests/me/GRADO/documents/TESIS/content")
                        .queryParam("disposition", "preview")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, program.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        verify(storage, never()).open(any());
        verify(objects, never()).open(any());
    }

    @Test
    @DisplayName("Scenario: A graduate header outside the caller scope is rejected")
    void graduateHeaderOutsideScopeIsRejected() throws Exception {
        Enrolled student = emanuel();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());
        clearInvocations(storage);

        mockMvc.perform(get("/api/exam-requests/me/GRADO/documents/TESIS/content")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, 99))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        verify(storage, never()).open(any());
    }

    @Test
    @DisplayName("Scenario: A blank graduate header still uses the JWT scope")
    void blankGraduateHeaderUsesTheJwtScope() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/exam-requests/me/GRADO/documents/TESIS/content")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, "   "))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    @Test
    @DisplayName("Scenario: An article stored as a link is not fetched")
    void articleLinkIsNotFetched() throws Exception {
        Enrolled student = doctoral();
        post(student, "GRADO", manifest(0, urlDescriptor("ARTICULO", ARTICLE))).andExpect(status().isOk());
        clearInvocations(storage);

        MvcResult result = content(student, "GRADO", "ARTICULO", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DOCUMENT_IS_EXTERNAL_URL"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(header().doesNotExist("Location"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(ARTICLE);
        verify(storage, never()).open(any());
        verify(objects, never()).open(any());
    }

    @Test
    @DisplayName("Scenario: An article stored as a PDF is streamed")
    void articlePdfIsStreamed() throws Exception {
        Enrolled student = doctoral();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "ARTICULO", "Articulo.pdf", pdf).andExpect(status().isOk());

        MvcResult result = content(student, "GRADO", "ARTICULO", null).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    @Test
    @DisplayName("Scenario: A saved PDF remains readable after the request leaves draft")
    void savedPdfRemainsReadableAfterDraft() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());

        for (String statusName : new String[] {"ENVIADA", "ACTUALIZADA", "CERRADA"}) {
            jdbc.update("""
                    UPDATE exam_requests
                    SET status = ?, folio = 'SEG-000001-000001', submitted_at = TIMESTAMP '2026-10-12 18:00:00'
                    WHERE student_id = ? AND exam_type = 'GRADO'
                    """, statusName, student.studentId());
            MvcResult result = content(student, "GRADO", "TESIS", null).andExpect(status().isOk()).andReturn();
            assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
        }
    }

    @Test
    @DisplayName("Scenario: A later academic status does not hide a stored PDF")
    void laterAcademicStatusDoesNotHideThePdf() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());
        int version = versionOf(student, "GRADO");
        int rows = requestRows();
        StudentProgram academicProgram = studentProgramRepository
                .findByStudentIdAndGraduateProgramIdWithAdvisors(student.studentId(), student.programId())
                .getFirst();
        academicProgram.updateMetadata(
                academicProgram.getAdmissionDate(), null, null, null, ProgramStatus.BAJA, "motivo", null);
        studentProgramRepository.save(academicProgram);
        clearInvocations(studentExamContextPort);

        MvcResult result = content(student, "GRADO", "TESIS", null).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("PROGRAM_NOT_ELIGIBLE");
        assertThat(versionOf(student, "GRADO")).isEqualTo(version);
        assertThat(requestRows()).isEqualTo(rows);
        verify(studentExamContextPort, never()).resolve(any(), any());
    }

    @Test
    @DisplayName("Scenario: Reading does not change the request")
    void readingDoesNotChangeTheRequest() throws Exception {
        Enrolled student = emanuel();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());
        Map<String, Object> before = requestRow(student, "GRADO");
        long documentId = currentDocumentId(student, "GRADO", "TESIS");
        clearInvocations(studentExamContextPort);

        content(student, "GRADO", "TESIS", null).andExpect(status().isOk());

        assertThat(requestRow(student, "GRADO")).isEqualTo(before);
        assertThat(before.get("folio")).isNull();
        assertThat(currentDocumentId(student, "GRADO", "TESIS")).isEqualTo(documentId);
        verify(studentExamContextPort, never()).resolve(any(), any());
    }

    @Test
    @DisplayName("Scenario: Predoctoral content without an own request is not found")
    void predoctoralWithoutAnOwnRequestIsNotFound() throws Exception {
        Enrolled student = emanuel();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());
        int rows = requestRows();

        content(student, "PREDOCTORAL", "TESIS", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.not("EXAM_TYPE_NOT_ALLOWED")));

        assertThat(requestRows()).isEqualTo(rows);
    }

    @Test
    @DisplayName("Scenario: An own predoctoral PDF is streamed")
    void ownPredoctoralPdfIsStreamed() throws Exception {
        Enrolled student = doctoral();
        byte[] grado = severalPages();
        byte[] predoctoral = onePage();
        postPdf(student, "GRADO", 0, "TESIS", DOCTORAL_TESIS, grado).andExpect(status().isOk());
        postPdf(student, "PREDOCTORAL", 0, "TESIS", DOCTORAL_TESIS, predoctoral).andExpect(status().isOk());

        MvcResult result = content(student, "PREDOCTORAL", "TESIS", null).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(predoctoral);
        assertThat(result.getResponse().getContentAsByteArray()).isNotEqualTo(grado);
    }

    @Test
    @DisplayName("Scenario: A Range header still returns the complete PDF")
    void rangeHeaderReturnsTheCompletePdf() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());

        MvcResult result = mockMvc.perform(authorized(student, get("/api/exam-requests/me/GRADO/documents/TESIS/content"))
                        .header("Range", "bytes=0-1023"))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Ranges", "none"))
                .andExpect(header().doesNotExist("Content-Range"))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pdf);
    }

    @Test
    @DisplayName("Scenario: An invalid disposition is rejected")
    void invalidDispositionIsRejected() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());

        MvcResult result = content(student, "GRADO", "TESIS", "preview")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("disposition"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("INVALID_ENUM"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("%PDF");
    }

    @Test
    @DisplayName("Scenario: An unknown document type is rejected")
    void unknownDocumentTypeIsRejected() throws Exception {
        Enrolled student = emanuel();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());
        clearInvocations(storage);

        content(student, "GRADO", "OTRO", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("documentType"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("INVALID_ENUM"));

        verify(storage, never()).open(any());
    }

    @Test
    @DisplayName("Scenario: A non-empty body is rejected")
    void nonEmptyBodyIsRejected() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());

        MvcResult result = mockMvc.perform(authorized(student, get("/api/exam-requests/me/GRADO/documents/TESIS/content"))
                        .content("{\"ignored\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("body"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("%PDF");
    }

    @Test
    @DisplayName("Scenario: Storage failure before headers returns JSON")
    void storageFailureBeforeHeadersReturnsJson() throws Exception {
        Enrolled student = emanuel();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());
        doAnswer(invocation -> {
            throw new ObjectStorageException("Object could not be opened");
        }).when(objects).open(any());

        MvcResult result = content(student, "GRADO", "TESIS", null)
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.error").value("DOCUMENT_STORAGE_UNAVAILABLE"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("%PDF");
    }

    @Test
    @DisplayName("A size mismatch before headers is storage unavailable")
    void sizeMismatchBeforeHeadersReturnsJson() throws Exception {
        Enrolled student = emanuel();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());
        doAnswer(invocation -> new OpenedObject(new ByteArrayInputStream(new byte[] {1}), 1L))
                .when(objects).open(any());

        content(student, "GRADO", "TESIS", null)
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.error").value("DOCUMENT_STORAGE_UNAVAILABLE"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.message").value("Document storage is unavailable"));
    }

    @Test
    @DisplayName("Scenario: A failure after headers aborts the stream")
    void failureAfterHeadersAbortsTheStream() throws Exception {
        Enrolled student = emanuel();
        byte[] pdf = severalPages();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, pdf).andExpect(status().isOk());
        doAnswer(invocation -> {
            byte[] bytes = stored.get(invocation.getArgument(0, String.class));
            InputStream failing = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("cut");
                }

                @Override
                public int read(byte[] buffer, int off, int len) throws IOException {
                    throw new IOException("cut");
                }
            };
            return new OpenedObject(failing, bytes.length);
        }).when(objects).open(any());

        MvcResult result = mockMvc.perform(authorized(student, get("/api/exam-requests/me/GRADO/documents/TESIS/content")))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).startsWith("application/pdf");
        assertThat(result.getResponse().getContentAsByteArray().length).isLessThan(pdf.length);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("DOCUMENT_STORAGE_UNAVAILABLE");
        assertThat(result.getResponse().getHeader("Content-Length")).isEqualTo(Integer.toString(pdf.length));
    }

    @Test
    @DisplayName("A persisted program outside the JWT scope is not found")
    void persistedProgramOutsideScopeIsNotFound() throws Exception {
        Enrolled student = emanuel();
        postPdf(student, "GRADO", 0, "TESIS", TESIS, severalPages()).andExpect(status().isOk());
        jdbc.update("UPDATE exam_requests SET graduate_program_id = graduate_program_id + 1 WHERE student_id = ?",
                student.studentId());
        clearInvocations(storage);

        content(student, "GRADO", "TESIS", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_FOUND"));

        verify(storage, never()).open(any());
    }

    private OpenedObject openStored(String key) {
        byte[] bytes = stored.get(key);
        if (bytes == null) {
            throw new ObjectStorageException("Object could not be opened");
        }
        return new OpenedObject(new ByteArrayInputStream(bytes), bytes.length);
    }

    private ResultActions content(Enrolled student, String examType, String documentType, String disposition)
            throws Exception {
        MockHttpServletRequestBuilder builder = authorized(
                student, get("/api/exam-requests/me/" + examType + "/documents/" + documentType + "/content"));
        if (disposition != null) {
            builder.queryParam("disposition", disposition);
        }
        return mockMvc.perform(builder);
    }

    private MockHttpServletRequestBuilder authorized(Enrolled student, MockHttpServletRequestBuilder builder) {
        return builder
                .header("Authorization", "Bearer " + student.token())
                .header(TenantFilter.HEADER_GRADUATE_ID, student.programId());
    }

    private ResultActions postPdf(
            Enrolled student, String examType, int version, String type, String filename, byte[] bytes)
            throws Exception {
        return post(student, examType, manifest(version, pdfDescriptor(type)), pdfPart(type, filename, bytes));
    }

    private ResultActions post(Enrolled student, String examType, String manifest, MockMultipartFile... files)
            throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/exam-requests/me/" + examType + "/documents")
                .file(new MockMultipartFile(
                        "manifest", "manifest.json", "application/json", manifest.getBytes(StandardCharsets.UTF_8)));
        for (MockMultipartFile file : files) {
            builder.file(file);
        }
        return mockMvc.perform(authorized(student, builder));
    }

    private ResultActions putThesis(Enrolled student, int version, String filename, byte[] bytes) throws Exception {
        return mockMvc.perform(authorized(student, multipart("/api/exam-requests/me/GRADO/documents/TESIS")
                .file(new MockMultipartFile(
                        "version", "", "text/plain", Integer.toString(version).getBytes(StandardCharsets.US_ASCII)))
                .file(new MockMultipartFile("file", filename, "application/pdf", bytes))
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })));
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

    private static byte[] severalPages() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            document.addPage(new PDPage(PDRectangle.A4));
            document.addPage(new PDPage(PDRectangle.A4));
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] onePage() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.LETTER));
            document.save(out);
            return out.toByteArray();
        }
    }

    private static String sha256(byte[] body) {
        try {
            byte[] raw = MessageDigest.getInstance("SHA-256").digest(body);
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte value : raw) {
                hex.append(Character.forDigit((value >> 4) & 0xf, 16));
                hex.append(Character.forDigit(value & 0xf, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String namesOf(String header) {
        int filename = header.indexOf("filename=");
        return header.substring(filename);
    }

    private static java.util.List<String> headerValues(MvcResult result) {
        return result.getResponse().getHeaderNames().stream()
                .map(name -> result.getResponse().getHeader(name))
                .toList();
    }

    private String objectKey(Enrolled student, String examType, String documentType) {
        return jdbc.queryForObject("""
                SELECT document.object_key FROM exam_request_documents document
                JOIN exam_requests request ON request.id = document.exam_request_id
                WHERE request.student_id = ? AND request.exam_type = ? AND document.document_type = ?
                """, String.class, student.studentId(), examType, documentType);
    }

    private String storedHash(Enrolled student, String examType, String documentType) {
        return jdbc.queryForObject("""
                SELECT document.sha256 FROM exam_request_documents document
                JOIN exam_requests request ON request.id = document.exam_request_id
                WHERE request.student_id = ? AND request.exam_type = ? AND document.document_type = ?
                """, String.class, student.studentId(), examType, documentType);
    }

    private int versionOf(Enrolled student, String examType) {
        return jdbc.queryForObject(
                "SELECT version FROM exam_requests WHERE student_id = ? AND exam_type = ?",
                Integer.class, student.studentId(), examType);
    }

    private long currentDocumentId(Enrolled student, String examType, String documentType) {
        return jdbc.queryForObject("""
                SELECT document.id FROM exam_request_documents document
                JOIN exam_requests request ON request.id = document.exam_request_id
                WHERE request.student_id = ? AND request.exam_type = ? AND document.document_type = ?
                """, Long.class, student.studentId(), examType, documentType);
    }

    private Map<String, Object> requestRow(Enrolled student, String examType) {
        return jdbc.queryForMap("""
                SELECT status, version, folio, graduate_program_id
                FROM exam_requests WHERE student_id = ? AND exam_type = ?
                """, student.studentId(), examType);
    }

    private int requestRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exam_requests", Integer.class);
    }

    private Enrolled emanuel() {
        return enroll(ProgramType.MAESTRIA, "Emanuel", "López", "Alba");
    }

    private Enrolled doctoral() {
        return enroll(ProgramType.DOCTORADO, "Ana", "Paz", "Sol");
    }

    private Enrolled enroll(ProgramType programType, String firstName, String paternal, String maternal) {
        GraduateProgram program = programAdapter.save(
                new GraduateProgram("Content " + programType + " " + System.nanoTime(), "CBI"));
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
        studentProgramRepository.save(StudentProgram.forRegistration(
                student.getId(), program.getId(), enrollmentId, programType, LocalDate.of(2024, 1, 15), null));
        return new Enrolled(program.getId(), student.getId(), login(user));
    }

    private User saveUser(RoleType role, Long programId) {
        String email = "content-" + role.name().toLowerCase() + "-" + System.nanoTime() + "@uam.mx";
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

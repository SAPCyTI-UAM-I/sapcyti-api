package mx.uam.sapcyti.exam.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamRequestBinding;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SPEC-047 and SPEC-049 contract for the student exam reads.
 * The current document and a mismatched binding replace {@link ExamRequestReadPort}; Flyway stays off.
 * Role checks use {@code SecurityConfig} ({@code integration-test}), not {@code TestSecurityConfig}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class ExamRequestControllerIT {

    private static final String PASSWORD = "password";

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
    private ExamRequestReadPort examRequestReadPort;

    @MockitoSpyBean
    private StudentExamContextPort studentExamContextPort;

    @BeforeEach
    void emptyDossier() {
        lenient().when(examRequestReadPort.findByStudentAndType(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Scenario: Specialization student sees five degree documents")
    void specializationSeesFiveDegreeDocuments() throws Exception {
        Enrolled student = enroll(ProgramType.ESPECIALIZACION, ProgramStatus.EN_INVESTIGACION, "Ana", "Ruiz", "Paz");
        int rowsBefore = examRequestRows();

        mockMvc.perform(get("/api/exam-requests/me/options")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().exists(TenantFilter.HEADER_REQUEST_ID))
                .andExpect(jsonPath("$.degree").value("ESPECIALIZACION"))
                .andExpect(jsonPath("$.programStatus").value("EN_INVESTIGACION"))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.eligibilityReason").value(nullValue()))
                .andExpect(jsonPath("$.examTypes.length()").value(1))
                .andExpect(jsonPath("$.examTypes[0]").value("GRADO"));

        MvcResult requirements = grado(student);
        assertThat(requirements.getResponse().getContentAsString()).doesNotContain("ARTICULO");
        assertThat(examRequestRows()).isEqualTo(rowsBefore);
    }

    @Test
    @DisplayName("Scenario: Master's student sees five degree documents")
    void masterSeesEmanuelFileNames() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");

        mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements.length()").value(5))
                .andExpect(jsonPath("$.requirements[?(@.documentType == 'ARTICULO')]").isEmpty())
                .andExpect(jsonPath("$.requirements[0].expectedName").value("Tesis_Mtria_Lopez_Alba_Emanuel.pdf"))
                .andExpect(jsonPath("$.requirements[0].exampleName").value("Tesis_Mtria_Lopez_Alba_Emanuel.pdf"))
                .andExpect(jsonPath("$.requirements[1].exampleName").value("Sol_Sinodales_Lopez_Alba_Emanuel.pdf"))
                .andExpect(jsonPath("$.requirements[2].exampleName").value("Hist_Acad_Lopez_Alba_Emanuel.pdf"))
                .andExpect(jsonPath("$.requirements[3].exampleName").value("Comp_Idioma_Lopez_Alba_Emanuel.pdf"))
                .andExpect(jsonPath("$.requirements[4].exampleName").value("Datos_Alum_Lopez_Alba_Emanuel.pdf"));
    }

    @Test
    @DisplayName("Scenario: Doctoral student sees six degree documents including the article")
    void doctorateSeesSixDocumentsAndPredoctoralOption() throws Exception {
        Enrolled student = enroll(ProgramType.DOCTORADO, ProgramStatus.EN_INVESTIGACION, "Elena", "Sosa", "Leal");

        mockMvc.perform(get("/api/exam-requests/me/options")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examTypes[0]").value("GRADO"))
                .andExpect(jsonPath("$.examTypes[1]").value("PREDOCTORAL"));

        mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements.length()").value(6))
                .andExpect(jsonPath("$.requirements[0].documentType").value("TESIS"))
                .andExpect(jsonPath("$.requirements[0].expectedName").value("Tesis_Docto_Sosa_Leal_Elena.pdf"))
                .andExpect(jsonPath("$.requirements[1].documentType").value("ARTICULO"))
                .andExpect(jsonPath("$.requirements[1].required").value(true))
                .andExpect(jsonPath("$.requirements[1].acceptedKinds[0]").value("PDF"))
                .andExpect(jsonPath("$.requirements[1].acceptedKinds[1]").value("URL"))
                .andExpect(jsonPath("$.requirements[1].expectedName").value(nullValue()))
                .andExpect(jsonPath("$.requirements[1].exampleName").value(nullValue()))
                .andExpect(jsonPath("$.requirements[2].documentType").value("SOL_SINODALES"))
                .andExpect(jsonPath("$.requirements[3].documentType").value("HIST_ACAD"))
                .andExpect(jsonPath("$.requirements[4].documentType").value("COMP_IDIOMA"))
                .andExpect(jsonPath("$.requirements[5].documentType").value("DATOS_ALUM"));
    }

    @Test
    @DisplayName("Scenario: Official names drop accents, ñ and internal spaces")
    void officialNamesDropAccents() throws Exception {
        Enrolled student = enroll(
                ProgramType.ESPECIALIZACION, ProgramStatus.EN_INVESTIGACION, "Ana María", "Muñoz", "Ibañez");

        mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[0].expectedName").value("Tesis_Esp_Munoz_Ibanez_Ana_Maria.pdf"))
                .andExpect(jsonPath("$.requirements[0].exampleName").value("Tesis_Esp_Munoz_Ibanez_Ana_Maria.pdf"));
    }

    @Test
    @DisplayName("Scenario: A missing maternal surname is omitted from the file name")
    void missingMaternalSurnameIsOmitted() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", null);

        mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements[0].expectedName").value("Tesis_Mtria_Lopez_Emanuel.pdf"))
                .andExpect(jsonPath("$.requirements[0].expectedName", not(org.hamcrest.Matchers.containsString("__"))));
    }

    @Test
    @DisplayName("Scenario: An egresado student is eligible")
    void egresadoIsEligible() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EGRESADO, "Emanuel", "López", "Alba");

        mockMvc.perform(get("/api/exam-requests/me/options")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.eligibilityReason").value(nullValue()));
    }

    @Test
    @DisplayName("Scenario: Baja or suspension can be read")
    void bajaAndSuspensionCanBeRead() throws Exception {
        Enrolled baja = enroll(ProgramType.MAESTRIA, ProgramStatus.BAJA, "Emanuel", "López", "Alba");
        mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + baja.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, baja.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.eligibilityReason").value("PROGRAM_BAJA"))
                .andExpect(jsonPath("$.requirements.length()").value(5));

        Enrolled suspension = enroll(ProgramType.MAESTRIA, ProgramStatus.SUSPENSION, "Elena", "Sosa", "Leal");
        mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + suspension.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, suspension.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.eligibilityReason").value("PROGRAM_SUSPENSION"))
                .andExpect(jsonPath("$.requirements.length()").value(5));
    }

    @Test
    @DisplayName("Scenario: A read without a request leaves every document pending")
    void missingRequestLeavesDocumentsPending() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");

        mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request").value(nullValue()))
                .andExpect(jsonPath("$.requirements[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.requirements[0].currentDocument").value(nullValue()))
                .andExpect(jsonPath("$.requirements[4].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.requirements[4].currentDocument").value(nullValue()));
    }

    @Test
    @DisplayName("Scenario: A current document is shown without its storage path")
    void currentDocumentOmitsStoragePath() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");
        when(examRequestReadPort.findByStudentAndType(student.studentId(), ExamType.GRADO))
                .thenReturn(Optional.of(new ExamRequestRead(
                        new ExamRequestBinding(
                                7L,
                                student.studentId(),
                                ExamType.GRADO,
                                student.programId(),
                                Degree.MAESTRIA,
                                ExamRequestStatus.BORRADOR,
                                null,
                                1,
                                null),
                        List.of(new CurrentExamDocument(
                                3L,
                                DocumentType.TESIS,
                                DocumentKind.PDF,
                                "Tesis_Mtria_Lopez_Alba_Emanuel.pdf",
                                null,
                                1200L,
                                Instant.parse("2026-03-01T15:04:05Z"))))));

        MvcResult result = mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.status").value("BORRADOR"))
                .andExpect(jsonPath("$.request.folio").value(nullValue()))
                .andExpect(jsonPath("$.requirements[0].status").value("CARGADO"))
                .andExpect(jsonPath("$.requirements[0].currentDocument.id").value(3))
                .andExpect(jsonPath("$.requirements[0].currentDocument.kind").value("PDF"))
                .andExpect(jsonPath("$.requirements[0].currentDocument.originalName")
                        .value("Tesis_Mtria_Lopez_Alba_Emanuel.pdf"))
                .andExpect(jsonPath("$.requirements[0].currentDocument.externalUrl").value(nullValue()))
                .andExpect(jsonPath("$.requirements[0].currentDocument.sizeBytes", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.requirements[1].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.requirements[4].status").value("PENDIENTE"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("storageKey", "bucket", "filesystem");
    }

    @Test
    @DisplayName("Scenario: A request tied to another program or degree is a conflict")
    void mismatchedBindingIsAConflict() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");
        when(examRequestReadPort.findByStudentAndType(student.studentId(), ExamType.GRADO))
                .thenReturn(Optional.of(new ExamRequestRead(
                        new ExamRequestBinding(
                                9L,
                                student.studentId(),
                                ExamType.GRADO,
                                student.programId() + 50,
                                Degree.MAESTRIA,
                                ExamRequestStatus.BORRADOR,
                                null,
                                1,
                                null),
                        List.of())));

        mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ACADEMIC_PROGRAM_CHANGED"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("Scenario: A caller who is not a student is denied")
    void coordinatorIsDeniedBeforeAnInvalidBody() throws Exception {
        GraduateProgram program = programAdapter.save(new GraduateProgram("Exam coord " + System.nanoTime(), "CBI"));
        String token = login(saveUser(RoleType.COORDINATOR, program.getId()));
        clearInvocations(studentExamContextPort);

        mockMvc.perform(get("/api/exam-requests/me/options")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, program.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"degree\":\"NO\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.details").doesNotExist());

        mockMvc.perform(get("/api/exam-requests/me/OTRO/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        verify(studentExamContextPort, never()).resolve(any(), any());
    }

    @Test
    @DisplayName("Scenario: A missing session is rejected before the academic read")
    void missingSessionDoesNotReadTheAcademicPort() throws Exception {
        clearInvocations(studentExamContextPort);

        mockMvc.perform(get("/api/exam-requests/me/options"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        verify(studentExamContextPort, never()).resolve(any(), any());
    }

    @Test
    @DisplayName("Scenario: A mismatched graduate scope is forbidden")
    void mismatchedGraduateScopeDoesNotReturnAnotherStudent() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");
        Enrolled other = enroll(ProgramType.DOCTORADO, ProgramStatus.EN_INVESTIGACION, "Secreto", "Ajeno", "Cruz");
        clearInvocations(studentExamContextPort);

        MvcResult result = mockMvc.perform(get("/api/exam-requests/me/options")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, other.programId()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.degree").doesNotExist())
                .andExpect(jsonPath("$.requirements").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("Secreto", "DOCTORADO");
        verify(studentExamContextPort, never()).resolve(any(), any());
    }

    @Test
    @DisplayName("Scenario: An ambiguous academic program is not guessed")
    void ambiguousProgramIsNotGuessed() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");
        studentProgramRepository.deleteAll(studentProgramRepository
                .findByStudentIdAndGraduateProgramIdWithAdvisors(student.studentId(), student.programId()));
        studentProgramRepository.save(StudentProgram.forRegistration(
                student.studentId(),
                student.programId(),
                "d" + student.studentId(),
                ProgramType.DOCTORADO,
                LocalDate.of(2024, 1, 15),
                null));

        mockMvc.perform(get("/api/exam-requests/me/options")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ACADEMIC_PROGRAM_AMBIGUOUS"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.examTypes").doesNotExist());
    }

    @Test
    @DisplayName("Scenario: Incomplete official names do not produce a file name")
    void incompleteNameDoesNotProduceAFileName() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "   ", "López", "Alba");

        MvcResult result = mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("STUDENT_DATA_INCOMPLETE"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("expectedName", "null");
    }

    @Test
    @DisplayName("Scenario: No student record for the authenticated user")
    void missingStudentRecord() throws Exception {
        GraduateProgram program = programAdapter.save(new GraduateProgram("Exam none " + System.nanoTime(), "CBI"));
        String token = login(saveUser(RoleType.STUDENT, program.getId()));

        mockMvc.perform(get("/api/exam-requests/me/options")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, program.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("STUDENT_NOT_FOUND"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("Scenario: Specialization or master's cannot request the predoctoral exam")
    void predoctoralIsRefused() throws Exception {
        Enrolled master = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");
        mockMvc.perform(get("/api/exam-requests/me/PREDOCTORAL/requirements")
                        .header("Authorization", "Bearer " + master.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, master.programId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("EXAM_TYPE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.details").doesNotExist());

        Enrolled specialization = enroll(
                ProgramType.ESPECIALIZACION, ProgramStatus.EN_INVESTIGACION, "Ana", "Ruiz", "Paz");
        mockMvc.perform(get("/api/exam-requests/me/PREDOCTORAL/requirements")
                        .header("Authorization", "Bearer " + specialization.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, specialization.programId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("EXAM_TYPE_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("Scenario: A doctoral student can choose the predoctoral exam")
    void doctoralStudentReadsPredoctoralRequirements() throws Exception {
        Enrolled student = enroll(ProgramType.DOCTORADO, ProgramStatus.EGRESADO, "Ana", "López", "Alba");
        int rowsBefore = examRequestRows();

        mockMvc.perform(get("/api/exam-requests/me/options")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.degree").value("DOCTORADO"))
                .andExpect(jsonPath("$.programStatus").value("EGRESADO"))
                .andExpect(jsonPath("$.examTypes.length()").value(2))
                .andExpect(jsonPath("$.examTypes[0]").value("GRADO"))
                .andExpect(jsonPath("$.examTypes[1]").value("PREDOCTORAL"));

        mockMvc.perform(get("/api/exam-requests/me/PREDOCTORAL/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.examType").value("PREDOCTORAL"))
                .andExpect(jsonPath("$.degree").value("DOCTORADO"))
                .andExpect(jsonPath("$.request").value(nullValue()))
                .andExpect(jsonPath("$.requirements.length()").value(6))
                .andExpect(jsonPath("$.requirements[0].documentType").value("TESIS"))
                .andExpect(jsonPath("$.requirements[0].required").value(true))
                .andExpect(jsonPath("$.requirements[0].acceptedKinds.length()").value(1))
                .andExpect(jsonPath("$.requirements[0].acceptedKinds[0]").value("PDF"))
                .andExpect(jsonPath("$.requirements[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.requirements[0].currentDocument").value(nullValue()))
                .andExpect(jsonPath("$.requirements[0].expectedName").value("Tesis_Docto_Lopez_Alba_Ana.pdf"))
                .andExpect(jsonPath("$.requirements[0].exampleName").value("Tesis_Docto_Lopez_Alba_Ana.pdf"))
                .andExpect(jsonPath("$.requirements[1].documentType").value("ARTICULO"))
                .andExpect(jsonPath("$.requirements[1].required").value(false))
                .andExpect(jsonPath("$.requirements[1].acceptedKinds[0]").value("PDF"))
                .andExpect(jsonPath("$.requirements[1].acceptedKinds[1]").value("URL"))
                .andExpect(jsonPath("$.requirements[1].expectedName").value(nullValue()))
                .andExpect(jsonPath("$.requirements[1].exampleName").value(nullValue()))
                .andExpect(jsonPath("$.requirements[1].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.requirements[1].currentDocument").value(nullValue()))
                .andExpect(jsonPath("$.requirements[2].documentType").value("SOL_SINODALES"))
                .andExpect(jsonPath("$.requirements[2].required").value(true))
                .andExpect(jsonPath("$.requirements[2].exampleName").value("Sol_Sinodales_Lopez_Alba_Ana.pdf"))
                .andExpect(jsonPath("$.requirements[3].documentType").value("HIST_ACAD"))
                .andExpect(jsonPath("$.requirements[3].exampleName").value("Hist_Acad_Lopez_Alba_Ana.pdf"))
                .andExpect(jsonPath("$.requirements[4].documentType").value("COMP_IDIOMA"))
                .andExpect(jsonPath("$.requirements[4].exampleName").value("Comp_Idioma_Lopez_Alba_Ana.pdf"))
                .andExpect(jsonPath("$.requirements[5].documentType").value("DATOS_ALUM"))
                .andExpect(jsonPath("$.requirements[5].exampleName").value("Datos_Alum_Lopez_Alba_Ana.pdf"));

        assertThat(examRequestRows()).isEqualTo(rowsBefore);
    }

    @Test
    @DisplayName("Scenario: Specialization cannot read the predoctoral exam")
    void specializationCannotReadThePredoctoralExam() throws Exception {
        Enrolled specialization = enroll(
                ProgramType.ESPECIALIZACION, ProgramStatus.EN_INVESTIGACION, "Ana", "Ruiz", "Paz");
        int rowsBefore = examRequestRows();

        mockMvc.perform(get("/api/exam-requests/me/PREDOCTORAL/requirements")
                        .header("Authorization", "Bearer " + specialization.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, specialization.programId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("EXAM_TYPE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.details").doesNotExist());

        assertThat(examRequestRows()).isEqualTo(rowsBefore);
    }

    @Test
    @DisplayName("Scenario: The degree exam stays a separate request")
    void degreeExamStaysASeparateRequest() throws Exception {
        Enrolled student = enroll(ProgramType.DOCTORADO, ProgramStatus.EGRESADO, "Ana", "López", "Alba");
        Instant uploadedAt = Instant.parse("2026-10-09T15:00:00Z");
        ExamRequestBinding predoctoral = new ExamRequestBinding(
                4L,
                student.studentId(),
                ExamType.PREDOCTORAL,
                student.programId(),
                Degree.DOCTORADO,
                ExamRequestStatus.ENVIADA,
                "SEG-482913-075316",
                2,
                uploadedAt);
        when(examRequestReadPort.findByStudentAndType(eq(student.studentId()), any())).thenAnswer(invocation -> {
            ExamType asked = invocation.getArgument(1);
            if (asked == ExamType.PREDOCTORAL) {
                return Optional.of(new ExamRequestRead(
                        predoctoral,
                        List.of(new CurrentExamDocument(
                                501L,
                                DocumentType.TESIS,
                                DocumentKind.PDF,
                                "PredoctoralOnly.pdf",
                                null,
                                102400L,
                                uploadedAt))));
            }
            return Optional.empty();
        });

        MvcResult result = mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.examType").value("GRADO"))
                .andExpect(jsonPath("$.request").value(nullValue()))
                .andExpect(jsonPath("$.requirements[1].documentType").value("ARTICULO"))
                .andExpect(jsonPath("$.requirements[1].required").value(true))
                .andExpect(jsonPath("$.requirements[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.requirements[0].currentDocument").value(nullValue()))
                .andExpect(jsonPath("$.requirements[1].currentDocument").value(nullValue()))
                .andExpect(jsonPath("$.requirements[2].currentDocument").value(nullValue()))
                .andExpect(jsonPath("$.requirements[3].currentDocument").value(nullValue()))
                .andExpect(jsonPath("$.requirements[4].currentDocument").value(nullValue()))
                .andExpect(jsonPath("$.requirements[5].currentDocument").value(nullValue()))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("PredoctoralOnly.pdf");
        assertThat(predoctoral.status()).isEqualTo(ExamRequestStatus.ENVIADA);
        assertThat(predoctoral.folio()).isEqualTo("SEG-482913-075316");
    }

    @Test
    @DisplayName("Scenario: An unknown exam type is a validation error")
    void unknownExamTypeIsInvalidEnum() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");
        clearInvocations(studentExamContextPort);

        mockMvc.perform(get("/api/exam-requests/me/OTRO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid exam type"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("examType"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("INVALID_ENUM"))
                .andExpect(jsonPath("$.details.fieldErrors[0].message").value("Use GRADO or PREDOCTORAL"))
                .andExpect(jsonPath("$.details.fieldErrors[0].message", not(org.hamcrest.Matchers.containsString("OTRO"))));

        verify(studentExamContextPort, never()).resolve(any(), any());
    }

    @Test
    @DisplayName("A body or query on either read is a validation error")
    void bodyOrQueryIsRejected() throws Exception {
        Enrolled student = enroll(ProgramType.MAESTRIA, ProgramStatus.EN_INVESTIGACION, "Emanuel", "López", "Alba");
        clearInvocations(studentExamContextPort);

        mockMvc.perform(get("/api/exam-requests/me/options")
                        .header("Authorization", "Bearer " + student.token())
                        .queryParam("degree", "MAESTRIA")
                        .content("{\"studentId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("query"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("INVALID_VALUE"))
                .andExpect(jsonPath("$.details.fieldErrors[1].field").value("body"))
                .andExpect(jsonPath("$.details.fieldErrors[1].code").value("INVALID_VALUE"))
                .andExpect(jsonPath("$.details.fieldErrors[0].message", not(
                        org.hamcrest.Matchers.containsString("MAESTRIA"))));

        mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .queryParam("status", "BAJA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("query"));

        verify(studentExamContextPort, never()).resolve(any(), any());
    }

    private MvcResult grado(Enrolled student) throws Exception {
        return mockMvc.perform(get("/api/exam-requests/me/GRADO/requirements")
                        .header("Authorization", "Bearer " + student.token())
                        .header(TenantFilter.HEADER_GRADUATE_ID, student.programId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.request").value(nullValue()))
                .andExpect(jsonPath("$.requirements.length()").value(5))
                .andExpect(jsonPath("$.requirements[0].documentType").value("TESIS"))
                .andExpect(jsonPath("$.requirements[0].required").value(true))
                .andExpect(jsonPath("$.requirements[0].acceptedKinds.length()").value(1))
                .andExpect(jsonPath("$.requirements[0].acceptedKinds[0]").value("PDF"))
                .andExpect(jsonPath("$.requirements[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.requirements[0].currentDocument").value(nullValue()))
                .andExpect(jsonPath("$.requirements[0].expectedName").value("Tesis_Esp_Ruiz_Paz_Ana.pdf"))
                .andExpect(jsonPath("$.requirements[1].documentType").value("SOL_SINODALES"))
                .andExpect(jsonPath("$.requirements[2].documentType").value("HIST_ACAD"))
                .andExpect(jsonPath("$.requirements[3].documentType").value("COMP_IDIOMA"))
                .andExpect(jsonPath("$.requirements[4].documentType").value("DATOS_ALUM"))
                .andReturn();
    }

    private Enrolled enroll(
            ProgramType programType, ProgramStatus status, String firstName, String paternal, String maternal) {
        GraduateProgram program = programAdapter.save(
                new GraduateProgram("Exam " + programType + " " + System.nanoTime(), "CBI"));
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
        String email = "exam-" + role.name().toLowerCase() + "-" + System.nanoTime() + "@uam.mx";
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

    private int examRequestRows() {
        Integer tables = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE LOWER(table_name) IN ('exam_request', 'exam_requests')
                """, Integer.class);
        if (tables == null || tables == 0) {
            return 0;
        }
        return jdbc.queryForObject("SELECT COUNT(*) FROM exam_requests", Integer.class);
    }

    private record Enrolled(Long programId, Long studentId, String token) {
    }
}

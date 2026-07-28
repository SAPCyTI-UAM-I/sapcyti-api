package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.internoProfessor;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.professorPersonalData;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterStudentRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.UpdateStudentRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataProfessorRepository;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataStudentRepository;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class StudentControllerIT {

    private static final String COORDINATOR_EMAIL = "coordinator-student@uam.mx";
    private static final String STUDENT_EMAIL = "student-role@uam.mx";
    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataStudentRepository studentRepository;

    @Autowired
    private SpringDataProfessorRepository professorRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    private Long programId;
    private Long advisorId;

    @BeforeEach
    void seed() {
        studentRepository.deleteAll();
        professorRepository.deleteAll();
        userRepository.deleteAll();

        GraduateProgram program = programAdapter.save(
                new GraduateProgram("PCyTI Student " + System.nanoTime(), "CBI"));
        programId = program.getId();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(PASSWORD);

        userRepository.save(new User(COORDINATOR_EMAIL, hash, RoleType.COORDINATOR, programId));
        userRepository.save(new User(STUDENT_EMAIL, hash, RoleType.STUDENT, programId));
        User advisorUser = userRepository.save(new User(
                "advisor.student@uam.mx", hash, RoleType.PROFESSOR, programId));

        Professor advisor = professorRepository.save(internoProfessor(
                "30568", advisorUser.getId(), programId, professorPersonalData()));
        advisorId = advisor.getId();
    }

    @Test
    @DisplayName("coordinator registers student and receives generated password")
    void registerSuccess() throws Exception {
        RegisterStudentRequest request = sampleRequest();

        MvcResult result = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/students/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.enrollmentId").value("2123803361"))
                .andExpect(jsonPath("$.email").value("paulina.valencia@uam.mx"))
                .andExpect(jsonPath("$.advisorId").value(advisorId.intValue()))
                .andExpect(jsonPath("$.birthDate").value("1998-03-15"))
                .andExpect(jsonPath("$.lastDegreeObtained").value("LICENCIATURA"))
                .andExpect(jsonPath("$.admissionTerm").value("23O"))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.generatedPassword").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String generatedPassword = body.get("generatedPassword").asText();

        User user = userRepository.findByEmail("paulina.valencia@uam.mx").orElseThrow();
        assertThat(new BCryptPasswordEncoder().matches(generatedPassword, user.getPasswordHash())).isTrue();
        assertThat(user.getRole()).isEqualTo(RoleType.STUDENT);
        assertThat(studentRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("registration without phoneExtension succeeds")
    void registerWithoutPhoneExtension() throws Exception {
        RegisterStudentRequest request = sampleRequestBuilder()
                .enrollmentId("2123803364")
                .email("no.extension@uam.mx")
                .phoneExtension(null)
                .build();

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneExtension").doesNotExist());
    }

    @Test
    @DisplayName("registration without advisor succeeds")
    void registerWithoutAdvisor() throws Exception {
        RegisterStudentRequest request = sampleRequestBuilder()
                .enrollmentId("2123803362")
                .email("no.advisor@uam.mx")
                .advisorId(null)
                .build();

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advisorId").doesNotExist());
    }

    @Test
    @DisplayName("registration without secondLastName succeeds")
    void registerWithoutSecondLastName() throws Exception {
        RegisterStudentRequest request = sampleRequestBuilder()
                .enrollmentId("2123803363")
                .email("no.second@uam.mx")
                .secondLastName(null)
                .build();

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secondLastName").doesNotExist());
    }

    @Test
    @DisplayName("duplicate email returns 409")
    void duplicateEmail() throws Exception {
        userRepository.save(new User(
                "paulina.valencia@uam.mx", "hash", RoleType.STUDENT, programId));

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email already exists"));
    }

    @Test
    @DisplayName("duplicate enrollment id returns 409")
    void duplicateEnrollmentId() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated());

        RegisterStudentRequest second = sampleRequestBuilder()
                .email("other.student@uam.mx")
                .advisorId(null)
                .build();

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "A student with this enrollment ID already exists"));
    }

    @Test
    @DisplayName("missing first name returns 400")
    void missingRequiredField() throws Exception {
        RegisterStudentRequest request = sampleRequestBuilder()
                .firstName("")
                .build();

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("invalid email returns 400")
    void invalidEmail() throws Exception {
        RegisterStudentRequest request = sampleRequestBuilder()
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email must be valid"));
    }

    @Test
    @DisplayName("unknown graduate program returns 404")
    void unknownProgram() throws Exception {
        programAdapter.deleteById(programId);

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Graduate program not found"));
    }

    @Test
    @DisplayName("non-existent advisor returns 404")
    void unknownAdvisor() throws Exception {
        RegisterStudentRequest request = sampleRequestBuilder()
                .advisorId(999L)
                .build();

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Professor not found"));
    }

    @Test
    @DisplayName("student role cannot register student")
    void unauthorizedRole() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET by id returns unified detail with embedded program")
    void getById() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(get("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value("2123803361"))
                .andExpect(jsonPath("$.admissionTerm").value("23O"))
                .andExpect(jsonPath("$.generatedPassword").doesNotExist())
                .andExpect(jsonPath("$.program.id").isNumber())
                .andExpect(jsonPath("$.program.enrollmentId").value("2123803361"));
    }

    @Test
    @DisplayName("HU-56: register accepts a missing admissionTerm (optional)")
    void registerAcceptsMissingAdmissionTerm() throws Exception {
        String body = objectMapper.writeValueAsString(sampleRequestBuilder().build())
                .replace(",\"admissionTerm\":\"23O\"", "");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admissionTerm").value(nullValue()));
    }

    @Test
    @DisplayName("HU-56: register rejects invalid admissionTerm format")
    void registerRejectsInvalidAdmissionTerm() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                sampleRequestBuilder().admissionTerm("26X").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("HU-56: register accepts lowercase admissionTerm and normalizes")
    void registerAcceptsLowercaseAdmissionTerm() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                sampleRequestBuilder().admissionTerm("26o").build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admissionTerm").value("26O"));
    }

    @Test
    @DisplayName("HU-56: PUT updates admissionTerm")
    void updateAdmissionTerm() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                sampleRequestBuilder().admissionTerm("26O").build())))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        UpdateStudentRequest updateRequest = new UpdateStudentRequest(
                "Paulina", "Valencia", "Franco", "paulina.valencia@uam.mx", "Mexicana",
                LocalDate.of(1998, 3, 15), "5554821234", "1234",
                "Computación", DegreeLevel.LICENCIATURA,
                ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), "26I", true);

        mockMvc.perform(put("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionTerm").value("26I"));

        mockMvc.perform(get("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionTerm").value("26I"));
    }

    @Test
    @DisplayName("PUT updates student personal data")
    void updateStudent() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        UpdateStudentRequest updateRequest = new UpdateStudentRequest(
                "Paulina",
                "Valencia Franco",
                "Franco",
                "paulina.updated@uam.mx",
                "Mexicana",
                LocalDate.of(1998, 3, 15),
                "5559998877",
                "4321",
                "Ingeniería en Computación",
                DegreeLevel.LICENCIATURA,
                ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1),
                "23O",
                true);

        mockMvc.perform(put("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("paulina.updated@uam.mx"))
                .andExpect(jsonPath("$.firstLastName").value("Valencia Franco"))
                .andExpect(jsonPath("$.enrollmentId").value("2123803361"));
    }

    @Test
    @DisplayName("PUT duplicate email returns 409")
    void updateDuplicateEmail() throws Exception {
        userRepository.save(new User("existing@uam.mx", "hash", RoleType.STUDENT, programId));

        MvcResult created = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequestBuilder()
                                .email("student.to.update@uam.mx")
                                .build())))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        UpdateStudentRequest updateRequest = new UpdateStudentRequest(
                "Paulina", "Valencia", "Franco", "existing@uam.mx", "Mexicana",
                LocalDate.of(1998, 3, 15), "5554821234", null,
                "Computación", DegreeLevel.LICENCIATURA,
                ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), "23O", true);

        mockMvc.perform(put("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email already exists"));
    }

    @Test
    @DisplayName("GET unknown student returns 404")
    void getByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/students/{id}", 999_999L)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found"));
    }

    @Test
    @DisplayName("GET list does not expose generatedPassword")
    void listWithoutPassword() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].enrollmentId").value("2123803361"))
                .andExpect(jsonPath("$.content[0].userId").isNumber())
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].generatedPassword").doesNotExist());
    }

    @Test
    @DisplayName("SPEC-032: free-text lastDegreeObtained on register returns 400")
    void registerRejectsFreeTextLastDegreeObtained() throws Exception {
        String body = objectMapper.writeValueAsString(sampleRequestBuilder().build())
                .replace("\"lastDegreeObtained\":\"LICENCIATURA\"",
                        "\"lastDegreeObtained\":\"Licenciatura en Computación\"");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("SPEC-032: free-text lastDegreeObtained on update returns 400")
    void updateRejectsFreeTextLastDegreeObtained() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        String body = objectMapper.writeValueAsString(new UpdateStudentRequest(
                "Paulina", "Valencia", "Franco", "paulina.valencia@uam.mx", "Mexicana",
                LocalDate.of(1998, 3, 15), "5554821234", null,
                "Computación", DegreeLevel.LICENCIATURA,
                ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), "23O", true))
                .replace("\"lastDegreeObtained\":\"LICENCIATURA\"",
                        "\"lastDegreeObtained\":\"Licenciatura en Computación\"");

        mockMvc.perform(put("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("SPEC-032: valid DegreeLevel DOCTORADO on update returns 200")
    void updateAcceptsValidDegreeLevel() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        UpdateStudentRequest updateRequest = new UpdateStudentRequest(
                "Paulina", "Valencia", "Franco", "paulina.valencia@uam.mx", "Mexicana",
                LocalDate.of(1998, 3, 15), "5554821234", null,
                "Computación", DegreeLevel.DOCTORADO,
                ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), "23O", true);

        mockMvc.perform(put("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastDegreeObtained").value("DOCTORADO"));
    }

    private RegisterStudentRequest sampleRequest() {
        return sampleRequestBuilder().build();
    }

    private RegisterStudentRequest.RegisterStudentRequestBuilder sampleRequestBuilder() {
        return RegisterStudentRequest.builder()
                .enrollmentId("2123803361")
                .email("paulina.valencia@uam.mx")
                .graduateProgramId(programId)
                .advisorId(advisorId)
                .firstName("Paulina")
                .firstLastName("Valencia")
                .secondLastName("Franco")
                .nationality("Mexicana")
                .birthDate(LocalDate.of(1998, 3, 15))
                .phone("5554821234")
                .phoneExtension("1234")
                .undergraduateDegree("Computación")
                .lastDegreeObtained(DegreeLevel.LICENCIATURA)
                .programType(ProgramType.MAESTRIA)
                .admissionDate(LocalDate.of(2023, 9, 1))
                .admissionTerm("23O");
    }

    private String coordinatorToken() throws Exception {
        return login(COORDINATOR_EMAIL);
    }

    private String studentToken() throws Exception {
        return login(STUDENT_EMAIL);
    }

    private String login(String email) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(PASSWORD)
                .rememberMe(false)
                .build();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}

package mx.uam.sapcyti.survey.infrastructure.adapter.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.studentPersonalData;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataStudentRepository;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.RegisterUeaRequest;
import mx.uam.sapcyti.offering.infrastructure.adapter.out.repository.SpringDataUeaRepository;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import mx.uam.sapcyti.survey.infrastructure.adapter.out.repository.SpringDataEnrollmentSurveyRepository;
import mx.uam.sapcyti.survey.infrastructure.adapter.out.repository.SpringDataSurveyResponseRepository;
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
class EnrollmentSurveyControllerIT {

    private static final String COORDINATOR_EMAIL = "survey-coordinator@uam.mx";
    private static final String STUDENT_EMAIL = "survey-student@uam.mx";
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
    private SpringDataUeaRepository ueaRepository;

    @Autowired
    private SpringDataEnrollmentSurveyRepository surveyRepository;

    @Autowired
    private SpringDataSurveyResponseRepository surveyResponseRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    private Long programId;
    private Long studentUserId;
    private long ueaId;

    @BeforeEach
    void seed() throws Exception {
        surveyResponseRepository.deleteAll();
        surveyRepository.deleteAll();
        studentRepository.deleteAll();
        ueaRepository.deleteAll();
        userRepository.deleteAll();

        GraduateProgram program = programAdapter.save(
                new GraduateProgram("PCyTI Survey " + System.nanoTime(), "CBI"));
        programId = program.getId();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(PASSWORD);
        userRepository.save(new User(COORDINATOR_EMAIL, hash, RoleType.COORDINATOR, programId));
        User studentUser = userRepository.save(new User(STUDENT_EMAIL, hash, RoleType.STUDENT, programId));
        studentUserId = studentUser.getId();

        studentRepository.save(new Student(
                "2123999001",
                studentUserId,
                programId,
                null,
                studentPersonalData(),
                sampleAcademicInformation()));

        ueaId = createUea("2156041", "MÉTODOS MATEMÁTICOS", 9);
    }

    @Test
    @DisplayName("coordinator creates survey in PROGRAMADO state")
    void createSurvey() throws Exception {
        long surveyId = createSurvey("26O", Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/enrollment-surveys/{id}", surveyId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.term").value("26O"))
                .andExpect(jsonPath("$.status").value("PROGRAMADO"))
                .andExpect(jsonPath("$.responseCount").value(0));
    }

    @Test
    @DisplayName("duplicate term returns SURVEY_ALREADY_EXISTS_FOR_TERM")
    void duplicateTerm() throws Exception {
        createSurvey("26O", Instant.now().plus(1, ChronoUnit.DAYS));

        ObjectNode body = surveyBody("26O", Instant.now().plus(2, ChronoUnit.DAYS));
        mockMvc.perform(post("/api/enrollment-surveys")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SURVEY_ALREADY_EXISTS_FOR_TERM"));
    }

    @Test
    @DisplayName("student submits and reads response on active survey")
    void studentSubmitAndRead() throws Exception {
        long surveyId = createActiveSurvey("26I");

        ObjectNode submit = objectMapper.createObjectNode();
        submit.put("academicTerm", "III");
        submit.put("mode", "ENROLL_UEAS");
        submit.set("ueaIds", objectMapper.createArrayNode().add(ueaId));

        mockMvc.perform(post("/api/enrollment-surveys/{id}/responses", surveyId)
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submit.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUeas").value(1))
                .andExpect(jsonPath("$.mode").value("ENROLL_UEAS"));

        mockMvc.perform(get("/api/enrollment-surveys/active")
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.survey.status").value("ACTIVO"))
                .andExpect(jsonPath("$.myResponse.totalUeas").value(1));

        mockMvc.perform(get("/api/enrollment-surveys/{id}/responses/me", surveyId)
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicTerm").value("III"));
    }

    @Test
    @DisplayName("coordinator queries results summary and demand")
    void coordinatorResults() throws Exception {
        long surveyId = createActiveSurvey("27P");
        submitStudentResponse(surveyId);

        mockMvc.perform(get("/api/enrollment-surveys/{id}/results/summary", surveyId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respondedCount").value(1))
                .andExpect(jsonPath("$.eligibleCount").value(1));

        mockMvc.perform(get("/api/enrollment-surveys/{id}/results/ueas", surveyId)
                        .param("sort", "totalResponses,desc")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalResponses").value(1))
                .andExpect(jsonPath("$[0].clave").value("2156041"));

        mockMvc.perform(get("/api/enrollment-surveys/{id}/results/ueas/{ueaId}/students", surveyId, ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value("2123999001"));
    }

    @Test
    @DisplayName("blank response counts as responded and shows up in blank-students")
    void coordinatorSeesBlankResponses() throws Exception {
        long surveyId = createActiveSurvey("27O");
        submitBlankResponse(surveyId);

        // Respondió pero en blanco: cuenta en el summary y no genera filas de demanda.
        mockMvc.perform(get("/api/enrollment-surveys/{id}/results/summary", surveyId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respondedCount").value(1))
                .andExpect(jsonPath("$.blankCount").value(1));

        mockMvc.perform(get("/api/enrollment-surveys/{id}/results/ueas", surveyId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/enrollment-surveys/{id}/results/blank-students", surveyId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value("2123999001"))
                .andExpect(jsonPath("$[0].academicTerm").value("III"));
    }

    @Test
    @DisplayName("deactivate UEA in active survey requires confirm")
    void deactivateUeaInActiveSurvey() throws Exception {
        createActiveSurvey("26O");

        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("UEA_IN_ACTIVE_SURVEY"));

        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", ueaId)
                        .param("confirm", "true")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("student cannot create survey")
    void studentForbidden() throws Exception {
        ObjectNode body = surveyBody("26O", Instant.now().plus(1, ChronoUnit.DAYS));
        mockMvc.perform(post("/api/enrollment-surveys")
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    private long createSurvey(String term, Instant opensAt) throws Exception {
        ObjectNode body = surveyBody(term, opensAt);
        MvcResult created = mockMvc.perform(post("/api/enrollment-surveys")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createActiveSurvey(String term) throws Exception {
        Instant opensAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant closesAt = Instant.now().plus(7, ChronoUnit.DAYS);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("term", term);
        body.put("opensAt", opensAt.toString());
        body.put("closesAt", closesAt.toString());
        MvcResult created = mockMvc.perform(post("/api/enrollment-surveys")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    }

    private void submitStudentResponse(long surveyId) throws Exception {
        ObjectNode submit = objectMapper.createObjectNode();
        submit.put("academicTerm", "II");
        submit.put("mode", "ENROLL_UEAS");
        submit.set("ueaIds", objectMapper.createArrayNode().add(ueaId));
        mockMvc.perform(post("/api/enrollment-surveys/{id}/responses", surveyId)
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submit.toString()))
                .andExpect(status().isOk());
    }

    private void submitBlankResponse(long surveyId) throws Exception {
        ObjectNode submit = objectMapper.createObjectNode();
        submit.put("academicTerm", "III");
        submit.put("mode", "BLANK");
        submit.set("ueaIds", objectMapper.createArrayNode());
        mockMvc.perform(post("/api/enrollment-surveys/{id}/responses", surveyId)
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submit.toString()))
                .andExpect(status().isOk());
    }

    private ObjectNode surveyBody(String term, Instant opensAt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("term", term);
        body.put("opensAt", opensAt.toString());
        body.put("closesAt", opensAt.plus(30, ChronoUnit.DAYS).toString());
        body.put("introMessage", "Mensaje de prueba");
        return body;
    }

    private long createUea(String clave, String nombre, int creditos) throws Exception {
        RegisterUeaRequest request = new RegisterUeaRequest(
                clave,
                nombre,
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.BASICA,
                creditos);
        MvcResult created = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
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

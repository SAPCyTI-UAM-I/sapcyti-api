package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.studentPersonalData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import mx.uam.sapcyti.planning.infrastructure.adapter.out.repository.SpringDataAnnualPlanRepository;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import mx.uam.sapcyti.survey.infrastructure.adapter.out.repository.SpringDataEnrollmentSurveyRepository;
import mx.uam.sapcyti.survey.infrastructure.adapter.out.repository.SpringDataSurveyResponseRepository;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.out.repository.SpringDataTrimestralPlanRepository;
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
class EnrollmentHistoryControllerIT {

    private static final String COORDINATOR_EMAIL = "history-coordinator@uam.mx";
    private static final String STUDENT_EMAIL = "history-student@uam.mx";
    private static final String STUDENT2_EMAIL = "history-student2@uam.mx";
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
    private SpringDataAnnualPlanRepository annualPlanRepository;

    @Autowired
    private SpringDataTrimestralPlanRepository trimestralPlanRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    private Long programId;
    private Long studentId;
    private Long student2Id;
    private long ueaId;

    @BeforeEach
    void seed() throws Exception {
        trimestralPlanRepository.deleteAll();
        surveyResponseRepository.deleteAll();
        surveyRepository.deleteAll();
        annualPlanRepository.deleteAll();
        studentRepository.deleteAll();
        ueaRepository.deleteAll();
        userRepository.deleteAll();

        GraduateProgram program = programAdapter.save(
                new GraduateProgram("PCyTI History " + System.nanoTime(), "CBI"));
        programId = program.getId();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(PASSWORD);
        userRepository.save(new User(COORDINATOR_EMAIL, hash, RoleType.COORDINATOR, programId));
        User studentUser = userRepository.save(new User(STUDENT_EMAIL, hash, RoleType.STUDENT, programId));
        User student2User = userRepository.save(new User(STUDENT2_EMAIL, hash, RoleType.STUDENT, programId));

        studentId = studentRepository
                .save(new Student(
                        "2123999201",
                        studentUser.getId(),
                        programId,
                        null,
                        studentPersonalData(),
                        sampleAcademicInformation()))
                .getId();
        student2Id = studentRepository
                .save(new Student(
                        "2123999202",
                        student2User.getId(),
                        programId,
                        null,
                        studentPersonalData(),
                        sampleAcademicInformation()))
                .getId();

        ueaId = createUea("2156041", "MÉTODOS MATEMÁTICOS");
    }

    @Test
    @DisplayName("survey without TERMINADA plan returns PENDING without group letters")
    void pendingWithoutTerminadaPlan() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O", studentToken(), false);

        mockMvc.perform(get("/api/students/{id}/enrollment-history", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].term").value("26O"))
                .andExpect(jsonPath("$[0].planStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].note").value("PENDING"))
                .andExpect(jsonPath("$[0].ueas[0].grupo").doesNotExist())
                .andExpect(jsonPath("$[0].ueas[0].professorName").doesNotExist())
                .andExpect(jsonPath("$[0].ueas[0].schedule").doesNotExist());
    }

    @Test
    @DisplayName("TERMINADA plan exposes group professor and schedule")
    void terminadaPlanExposesAssignments() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O", studentToken(), false);
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);
        saveGroupsWithSchedule(planId);
        markTerminada(planId);

        mockMvc.perform(get("/api/students/{id}/enrollment-history", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].term").value("26O"))
                .andExpect(jsonPath("$[0].planStatus").value("TERMINADA"))
                .andExpect(jsonPath("$[0].ueas[0].grupo").isNotEmpty())
                .andExpect(jsonPath("$[0].ueas[0].schedule[0].day").value("LUN"));
    }

    @Test
    @DisplayName("BLANK enrollment under TERMINADA has empty ueas")
    void blankUnderTerminada() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O", studentToken(), true);
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);
        markTerminada(planId);

        mockMvc.perform(get("/api/students/{id}/enrollment-history", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planStatus").value("TERMINADA"))
                .andExpect(jsonPath("$[0].ueas").isEmpty());
    }

    @Test
    @DisplayName("manual student without survey gets MANUAL_NOT_SURVEYED")
    void manualWithoutSurvey() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O", studentToken(), false);
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);
        addManualStudent(planId);
        markTerminada(planId);

        mockMvc.perform(get("/api/students/{id}/enrollment-history", student2Id)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].academicTermSelected").isEmpty())
                .andExpect(jsonPath("$[0].note").value("MANUAL_NOT_SURVEYED"))
                .andExpect(jsonPath("$[0].ueas[0].grupo").isNotEmpty());
    }

    @Test
    @DisplayName("entries are ordered newest term first")
    void newestTermFirst() throws Exception {
        createClosedSurveyWithResponse("25O", studentToken(), false);
        createClosedSurveyWithResponse("26O", studentToken(), false);

        mockMvc.perform(get("/api/students/{id}/enrollment-history", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].term").value("26O"))
                .andExpect(jsonPath("$[1].term").value("25O"));
    }

    @Test
    @DisplayName("student without history returns empty array")
    void emptyHistory() throws Exception {
        mockMvc.perform(get("/api/students/{id}/enrollment-history", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("missing student returns 404")
    void studentNotFound() throws Exception {
        mockMvc.perform(get("/api/students/{id}/enrollment-history", 99999L)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isNotFound());
    }

    private void addManualStudent(long planId) throws Exception {
        MvcResult detail = mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode group = objectMapper.readTree(detail.getResponse().getContentAsString()).get("groups").get(0);

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode groups = body.putArray("groups");
        ObjectNode g = groups.addObject();
        g.put("id", group.get("id").asLong());
        g.put("ueaId", group.get("ueaId").asLong());
        g.put("grupo", group.get("grupo").asText());
        g.put("cupo", group.get("cupo").asText());
        g.putNull("professorId");
        g.putNull("obs");
        ArrayNode schedule = g.putArray("schedule");
        for (String day : new String[] {"LUN", "MAR", "MIE", "JUE", "VIE"}) {
            ObjectNode slot = schedule.addObject();
            slot.put("day", day);
            slot.putNull("start");
            slot.putNull("end");
            slot.put("lab", false);
        }
        ArrayNode students = g.putArray("studentIds");
        students.add(studentId);
        students.add(student2Id);

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk());
    }

    private void saveGroupsWithSchedule(long planId) throws Exception {
        MvcResult detail = mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode group = objectMapper.readTree(detail.getResponse().getContentAsString()).get("groups").get(0);

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode groups = body.putArray("groups");
        ObjectNode g = groups.addObject();
        g.put("id", group.get("id").asLong());
        g.put("ueaId", group.get("ueaId").asLong());
        g.put("grupo", "CO99");
        g.put("cupo", group.get("cupo").asText());
        g.putNull("professorId");
        g.putNull("obs");
        ArrayNode schedule = g.putArray("schedule");
        for (String day : new String[] {"LUN", "MAR", "MIE", "JUE", "VIE"}) {
            ObjectNode slot = schedule.addObject();
            slot.put("day", day);
            slot.put("start", "08:00");
            slot.put("end", "10:00");
            slot.put("lab", "LUN".equals(day));
        }
        g.putArray("studentIds").add(studentId);

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk());
    }

    private void markTerminada(long planId) throws Exception {
        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk());
    }

    private long generatePlan(long surveyId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createClosedSurveyWithResponse(String term, String token, boolean blank) throws Exception {
        long surveyId = createActiveSurvey(term);
        if (blank) {
            submitBlankResponse(surveyId, token);
        } else {
            submitEnrollResponse(surveyId, token);
        }
        closeSurvey(surveyId);
        return surveyId;
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

    private void closeSurvey(long surveyId) throws Exception {
        mockMvc.perform(put("/api/enrollment-surveys/{id}/close", surveyId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk());
    }

    private void submitEnrollResponse(long surveyId, String token) throws Exception {
        ObjectNode submit = objectMapper.createObjectNode();
        submit.put("academicTerm", "I");
        submit.put("mode", "ENROLL_UEAS");
        submit.set("ueaIds", objectMapper.createArrayNode().add(ueaId));
        mockMvc.perform(post("/api/enrollment-surveys/{id}/responses", surveyId)
                        .header("Authorization", "Bearer " + token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submit.toString()))
                .andExpect(status().isOk());
    }

    private void submitBlankResponse(long surveyId, String token) throws Exception {
        ObjectNode submit = objectMapper.createObjectNode();
        submit.put("academicTerm", "I");
        submit.put("mode", "BLANK");
        submit.set("ueaIds", objectMapper.createArrayNode());
        mockMvc.perform(post("/api/enrollment-surveys/{id}/responses", surveyId)
                        .header("Authorization", "Bearer " + token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submit.toString()))
                .andExpect(status().isOk());
    }

    private void createAnnualPlanWithCupo(int year, String cupoO) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/annual-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":" + year + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        long entryId = body.get("entries").get(0).get("id").asLong();

        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode entries = payload.putArray("entries");
        ObjectNode entry = entries.addObject();
        entry.put("id", entryId);
        entry.put("cupoO", cupoO);
        entry.putObject("marks");

        mockMvc.perform(put("/api/annual-plans/{year}/entries", year)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isOk());
    }

    private long createUea(String clave, String nombre) throws Exception {
        RegisterUeaRequest request = new RegisterUeaRequest(
                clave,
                nombre,
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.BASICA,
                9);
        MvcResult created = mockMvc.perform(post("/api/ueas")
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
        LoginRequest request = LoginRequest.builder().email(email).password(PASSWORD).build();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}

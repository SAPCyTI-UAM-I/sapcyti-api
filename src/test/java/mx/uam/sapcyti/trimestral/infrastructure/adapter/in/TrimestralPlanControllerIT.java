package mx.uam.sapcyti.trimestral.infrastructure.adapter.in;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.studentPersonalData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
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
class TrimestralPlanControllerIT {

    private static final String COORDINATOR_EMAIL = "trimestral-coordinator@uam.mx";
    private static final String STUDENT_EMAIL = "trimestral-student@uam.mx";
    private static final String STUDENT2_EMAIL = "trimestral-student2@uam.mx";
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
                new GraduateProgram("PCyTI Trimestral " + System.nanoTime(), "CBI"));
        programId = program.getId();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(PASSWORD);
        userRepository.save(new User(COORDINATOR_EMAIL, hash, RoleType.COORDINATOR, programId));
        User studentUser = userRepository.save(new User(STUDENT_EMAIL, hash, RoleType.STUDENT, programId));
        User student2User = userRepository.save(new User(STUDENT2_EMAIL, hash, RoleType.STUDENT, programId));

        studentId = studentRepository
                .save(new Student(
                        "2123999101",
                        studentUser.getId(),
                        programId,
                        null,
                        studentPersonalData(),
                        sampleAcademicInformation()))
                .getId();
        student2Id = studentRepository
                .save(new Student(
                        "2123999102",
                        student2User.getId(),
                        programId,
                        null,
                        new PersonalData("Bruno", "Diaz", "Luna", "Mexicana", null, "5554829999", null),
                        sampleAcademicInformation()))
                .getId();

        ueaId = createUea("2156041", "MÉTODOS MATEMÁTICOS");
    }

    @Test
    @DisplayName("generates BORRADOR from closed survey with annual plan")
    void generatePlan() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");

        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BORRADOR"))
                .andExpect(jsonPath("$.term").value("26O"))
                .andExpect(jsonPath("$.surveyId").value(surveyId))
                .andExpect(jsonPath("$.groups.length()").value(1))
                .andExpect(jsonPath("$.groups[0].cupo").value("25"))
                .andExpect(jsonPath("$.groups[0].grupo").value("CO43"))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    @DisplayName("rejects generation when survey is not closed")
    void surveyNotClosed() throws Exception {
        long surveyId = createActiveSurvey("26O");
        createAnnualPlanWithCupo(2026, "25");

        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SURVEY_NOT_CLOSED"));
    }

    @Test
    @DisplayName("rejects generation without annual plan")
    void annualPlanRequired() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");

        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ANNUAL_PLAN_REQUIRED"));
    }

    @Test
    @DisplayName("rejects duplicate term")
    void duplicateTerm() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        generatePlan(surveyId);

        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRIMESTRAL_PLAN_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("missing survey returns SURVEY_NOT_FOUND")
    void surveyNotFound() throws Exception {
        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":99999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SURVEY_NOT_FOUND"));
    }

    @Test
    @DisplayName("list and get plan; save groups; status cycle; regenerate")
    void editStatusAndRegenerate() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        mockMvc.perform(get("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].term").value("26O"))
                .andExpect(jsonPath("$[0].groupCount").value(1));

        mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].students[0].studentId").value(studentId));

        ObjectNode saveBody = emptyScheduleSaveBody(planId, "1");
        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[?(@.code=='CUPO_EXCEEDED')]").exists());

        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINADA"));

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRIMESTRAL_PLAN_NOT_EDITABLE"));

        mockMvc.perform(post("/api/trimestral-plans/{id}/regenerate", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRIMESTRAL_PLAN_NOT_EDITABLE"));

        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BORRADOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BORRADOR"));

        mockMvc.perform(post("/api/trimestral-plans/{id}/regenerate", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outdated").value(false))
                .andExpect(jsonPath("$.groups[0].cupo").value("25"));
    }

    @Test
    @DisplayName("BORRADOR check runs before format validation")
    void editableBeforeFormat() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk());

        ObjectNode saveBody = emptyScheduleSaveBody(planId, "0");
        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRIMESTRAL_PLAN_NOT_EDITABLE"));
    }

    @Test
    @DisplayName("reopen survey blocked by TERMINADA plan; marks outdated on BORRADOR")
    void surveyReopenGate() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk());

        ObjectNode reopen = objectMapper.createObjectNode();
        reopen.put("term", "26O");
        reopen.put("opensAt", Instant.now().minus(1, ChronoUnit.HOURS).toString());
        reopen.put("closesAt", Instant.now().plus(10, ChronoUnit.DAYS).toString());
        reopen.put("introMessage", "reopen");

        mockMvc.perform(put("/api/enrollment-surveys/{id}", surveyId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopen.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SURVEY_REOPEN_BLOCKED_TERMINATED_PLAN"));

        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BORRADOR\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/enrollment-surveys/{id}", surveyId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopen.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outdated").value(true));
    }

    @Test
    @DisplayName("cross-tenant studentId is rejected")
    void crossTenantStudentRejected() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        GraduateProgram other = programAdapter.save(new GraduateProgram("Other " + System.nanoTime(), "CBI"));
        User otherUser = userRepository.save(new User(
                "other-student@uam.mx", new BCryptPasswordEncoder().encode(PASSWORD), RoleType.STUDENT, other.getId()));
        Long foreignStudentId = studentRepository
                .save(new Student(
                        "9999999999",
                        otherUser.getId(),
                        other.getId(),
                        null,
                        studentPersonalData(),
                        sampleAcademicInformation()))
                .getId();

        ObjectNode saveBody = emptyScheduleSaveBody(planId, "25");
        ((ArrayNode) saveBody.get("groups").get(0).get("studentIds")).removeAll().add(foreignStudentId);

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("blank-only survey yields NO_RESPONSES")
    void blankOnlySurvey() throws Exception {
        long surveyId = createActiveSurvey("26O");
        submitBlankResponse(surveyId);
        closeSurvey(surveyId);
        createAnnualPlanWithCupo(2026, "25");

        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groups.length()").value(0))
                .andExpect(jsonPath("$.blankStudents.length()").value(1))
                .andExpect(jsonPath("$.warnings[0].code").value("NO_RESPONSES"));
    }

    @Test
    @DisplayName("non-coordinator cannot list plans")
    void studentForbidden() throws Exception {
        mockMvc.perform(get("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HU-60: export BORRADOR plan returns official Excel")
    void exportBorradorPlan() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        mockMvc.perform(get("/api/trimestral-plans/{id}/export", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(
                        "Content-Disposition", "attachment; filename=\"PCYTI 26O.xlsx\""));
    }

    @Test
    @DisplayName("HU-60: export TERMINADA plan is allowed")
    void exportTerminadaPlan() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trimestral-plans/{id}/export", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU-60: missing plan export returns TRIMESTRAL_PLAN_NOT_FOUND")
    void exportMissingPlan() throws Exception {
        mockMvc.perform(get("/api/trimestral-plans/{id}/export", 99999L)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TRIMESTRAL_PLAN_NOT_FOUND"));
    }

    @Test
    @DisplayName("HU-60: blank students are not exported")
    void exportOmitsBlankStudents() throws Exception {
        long surveyId = createActiveSurvey("26O");
        submitBlankResponse(surveyId);
        closeSurvey(surveyId);
        createAnnualPlanWithCupo(2026, "25");

        MvcResult created = mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long planId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        MvcResult exported = mockMvc.perform(get("/api/trimestral-plans/{id}/export", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andReturn();

        try (var workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(
                new ByteArrayInputStream(exported.getResponse().getContentAsByteArray()))) {
            var sheet = workbook.getSheet("Original CyTI");
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1);
        }
    }

    private ObjectNode emptyScheduleSaveBody(long planId, String cupo) throws Exception {
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
        g.put("cupo", cupo);
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
        return body;
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

    private long createClosedSurveyWithResponse(String term) throws Exception {
        long surveyId = createActiveSurvey(term);
        submitEnrollResponse(surveyId);
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

    private void submitEnrollResponse(long surveyId) throws Exception {
        ObjectNode submit = objectMapper.createObjectNode();
        submit.put("academicTerm", "I");
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
        submit.put("academicTerm", "I");
        submit.put("mode", "BLANK");
        submit.set("ueaIds", objectMapper.createArrayNode());
        mockMvc.perform(post("/api/enrollment-surveys/{id}/responses", surveyId)
                        .header("Authorization", "Bearer " + studentToken())
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

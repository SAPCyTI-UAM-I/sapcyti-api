package mx.uam.sapcyti.trimestral.infrastructure.adapter.in;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.internoProfessor;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.minimalProfessorPersonalData;
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
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataProfessorRepository;
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
    private SpringDataProfessorRepository professorRepository;

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
        professorRepository.deleteAll();
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
    @DisplayName("rejects generation while the annual plan remains BORRADOR")
    void annualPlanMustBeTerminated() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanDraftWithCapacity(2026, "*", "25");

        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ANNUAL_PLAN_NOT_TERMINATED"));
    }

    @Test
    @DisplayName("all trimestral mutations are blocked while annual plan is not TERMINADA; GET remains available")
    void annualPrerequisiteGuardsEveryMutation() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);
        ObjectNode saveBody = saveBodyFromDetail(getPlanDetail(planId));

        mockMvc.perform(patch("/api/annual-plans/{year}/status", 2026)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BORRADOR\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outdatedReasons[0]").value("ANNUAL_PLAN_CHANGED"))
                .andExpect(jsonPath("$.prerequisites.annualPlanTerminated").value(false));

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ANNUAL_PLAN_NOT_TERMINATED"));
        mockMvc.perform(post("/api/trimestral-plans/{id}/regenerate", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ANNUAL_PLAN_NOT_TERMINATED"));
        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ANNUAL_PLAN_NOT_TERMINATED"));
        mockMvc.perform(get("/api/trimestral-plans/{id}/export", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ANNUAL_PLAN_NOT_TERMINATED"));
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

        ObjectNode saveBody = emptyScheduleSaveBody(planId, "25");
        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[?(@.code=='CUPO_EXCEEDED')]").doesNotExist())
                .andExpect(jsonPath("$.groups[0].students[0].obs").value("Maestría Física"))
                .andExpect(jsonPath("$.groups[0].students[1].obs").isEmpty());

        // student obs persisted and returned on GET (students sorted by surname: Diaz before Valencia)
        mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].students[0].studentId").value(student2Id))
                .andExpect(jsonPath("$.groups[0].students[0].obs").value("Maestría Física"))
                .andExpect(jsonPath("$.groups[0].students[1].obs").isEmpty());

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
                .andExpect(jsonPath("$.groups[0].cupo").value("25"))
                .andExpect(jsonPath("$.groups[0].students[0].obs").isEmpty());
    }

    @Test
    @DisplayName("new group inherits annual capacity when cupo is omitted")
    void newGroupInheritsAnnualCapacity() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        ObjectNode saveBody = saveBodyFromDetail(getPlanDetail(planId));
        ObjectNode group = (ObjectNode) saveBody.get("groups").get(0);
        group.putNull("id");
        group.putNull("cupo");

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].cupo").value("25"))
                .andExpect(jsonPath("$.groups[0].maxGroups").value("*"));
    }

    @Test
    @DisplayName("explicit group capacity must match the annual plan")
    void explicitGroupCapacityMustMatchAnnualPlan() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        ObjectNode saveBody = saveBodyFromDetail(getPlanDetail(planId));
        ((ObjectNode) saveBody.get("groups").get(0)).put("cupo", "10");

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("manual removal becomes unassigned demand and history labels the removed UEA")
    void manualRemovalReconcilesUnassignedDemandAndHistory() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        JsonNode detail = getPlanDetail(planId);
        ObjectNode saveBody = saveBodyFromDetail(detail);
        ((ArrayNode) saveBody.get("groups").get(0).get("students")).removeAll();

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unassignedDemand.length()").value(1))
                .andExpect(jsonPath("$.unassignedDemand[0].studentId").value(studentId))
                .andExpect(jsonPath("$.unassignedDemand[0].reason").value("MANUALLY_UNASSIGNED"));

        markTerminada(planId);
        mockMvc.perform(get("/api/students/{id}/enrollment-history", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ueas[0].status").value("REMOVED_FROM_FINAL_PLAN"))
                .andExpect(jsonPath("$[0].ueas[0].grupo").isEmpty())
                .andExpect(jsonPath("$[0].ueas[0].professors").isEmpty())
                .andExpect(jsonPath("$[0].ueas[0].schedule").isEmpty());
    }

    @Test
    @DisplayName("persists and reloads ordered co-directors")
    void multipleProfessorsRoundTrip() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);
        Professor first = createProfessor("41001", "Ada", "Lovelace", "ada-prof@uam.mx");
        Professor second = createProfessor("41002", "Grace", "Hopper", "grace-prof@uam.mx");

        ObjectNode saveBody = emptyScheduleSaveBody(planId, "25");
        ArrayNode professorIds = (ArrayNode) saveBody.get("groups").get(0).get("professorIds");
        professorIds.add(first.getId()).add(second.getId());

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].professors[0].professorId").value(first.getId()))
                .andExpect(jsonPath("$.groups[0].professors[1].professorId").value(second.getId()));

        // A separate GET crosses the repository transaction boundary and catches lazy
        // collection regressions that an in-memory save response would hide.
        mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].professors.length()").value(2))
                .andExpect(jsonPath("$.groups[0].professors[0].employeeNumber").value("41001"))
                .andExpect(jsonPath("$.groups[0].professors[1].employeeNumber").value("41002"));

        markTerminada(planId);
        mockMvc.perform(get("/api/students/{id}/enrollment-history", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ueas[0].professors.length()").value(2))
                .andExpect(jsonPath("$[0].ueas[0].professors[0].professorId").value(first.getId()))
                .andExpect(jsonPath("$[0].ueas[0].professors[0].employeeNumber").value("41001"))
                .andExpect(jsonPath("$[0].ueas[0].professors[1].professorName").value("Grace Hopper"));
    }

    @Test
    @DisplayName("keeps an already assigned inactive professor but rejects a new inactive assignment")
    void inactiveProfessorAssignmentRules() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);
        Professor assigned = createProfessor("42001", "Dorothy", "Vaughan", "assigned-prof@uam.mx");

        ObjectNode initial = emptyScheduleSaveBody(planId, "25");
        ((ArrayNode) initial.get("groups").get(0).get("professorIds")).add(assigned.getId());
        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initial.toString()))
                .andExpect(status().isOk());

        User assignedUser = userRepository.findById(assigned.getUserId()).orElseThrow();
        assignedUser.setActive(false);
        userRepository.save(assignedUser);

        JsonNode detail = getPlanDetail(planId);
        ObjectNode unchanged = saveBodyFromDetail(detail);
        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unchanged.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].professors[0].employeeNumber").value("42001"))
                .andExpect(jsonPath("$.warnings[?(@.code=='PROFESSOR_INACTIVE')]").exists());

        Professor neverAssigned = createProfessor("42002", "Katherine", "Johnson", "new-inactive@uam.mx");
        User newUser = userRepository.findById(neverAssigned.getUserId()).orElseThrow();
        newUser.setActive(false);
        userRepository.save(newUser);
        JsonNode refreshed = getPlanDetail(planId);
        ObjectNode invalid = saveBodyFromDetail(refreshed);
        ((ArrayNode) invalid.get("groups").get(0).get("professorIds")).add(neverAssigned.getId());

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("keeps an inactive student in place but rejects moving that student")
    void inactiveStudentAssignmentRules() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);
        JsonNode detail = getPlanDetail(planId);

        Student student = studentRepository.findById(studentId).orElseThrow();
        User studentUser = userRepository.findById(student.getUserId()).orElseThrow();
        studentUser.setActive(false);
        userRepository.save(studentUser);

        ObjectNode unchanged = saveBodyFromDetail(detail);
        MvcResult kept = mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unchanged.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[?(@.code=='STUDENT_INACTIVE')]").exists())
                .andReturn();

        JsonNode refreshed = objectMapper.readTree(kept.getResponse().getContentAsString());
        ObjectNode moved = saveBodyFromDetail(refreshed);
        ArrayNode groups = (ArrayNode) moved.get("groups");
        ObjectNode original = (ObjectNode) groups.get(0);
        ObjectNode destination = original.deepCopy();
        destination.remove("id");
        destination.put("grupo", "CO43A");
        ((ArrayNode) original.get("students")).removeAll();
        groups.add(destination);

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moved.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("mixes ordinary UEA demand in one base and still saves a student taking several UEAs")
    void mixedTermsAndMultipleUeasRoundTrip() throws Exception {
        long secondUeaId = createUea("2156042", "OPTIMIZACIÓN");
        long surveyId = createActiveSurvey("26O");
        submitEnrollResponse(surveyId, studentToken(), "I", ueaId, secondUeaId);
        submitEnrollResponse(surveyId, login(STUDENT2_EMAIL), "II", ueaId);
        closeSurvey(surveyId);
        createAnnualPlanWithCupo(2026, "25");

        MvcResult generated = mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode detail = objectMapper.readTree(generated.getResponse().getContentAsString());
        java.util.List<String> firstUeaGroups = new java.util.ArrayList<>();
        for (JsonNode group : detail.get("groups")) {
            if (group.get("ueaId").asLong() == ueaId) {
                firstUeaGroups.add(group.get("grupo").asText());
            }
        }
        assertThat(firstUeaGroups).containsExactly("CO43");
        long planId = detail.get("id").asLong();
        ObjectNode saveBody = saveBodyFromDetail(detail);
        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unassignedDemand[?(@.ueaId==" + secondUeaId + ")].studentId")
                        .value(studentId.intValue()))
                .andExpect(jsonPath("$.unassignedDemand[?(@.ueaId==" + secondUeaId + ")].reason")
                        .value("UEA_NOT_OFFERED"));
    }

    @Test
    @DisplayName("enforces authorized annual groups and persists overflow as unassigned demand")
    void annualCapacityDistribution() throws Exception {
        String thirdToken = createStudentAndLogin(
                "trimestral-student3@uam.mx", "2123999103", "Clara", "Evans", "Smith");
        long surveyId = createActiveSurvey("26O");
        submitEnrollResponse(surveyId, studentToken(), "I", ueaId);
        submitEnrollResponse(surveyId, login(STUDENT2_EMAIL), "I", ueaId);
        submitEnrollResponse(surveyId, thirdToken, "I", ueaId);
        closeSurvey(surveyId);
        createAnnualPlanWithCapacity(2026, "1", "2");

        MvcResult created = mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groups.length()").value(1))
                .andExpect(jsonPath("$.groups[0].grupo").value("CO43"))
                .andExpect(jsonPath("$.groups[0].students.length()").value(2))
                .andExpect(jsonPath("$.unassignedDemand.length()").value(1))
                .andExpect(jsonPath("$.unassignedDemand[0].reason").value("GROUP_LIMIT_REACHED"))
                .andReturn();

        JsonNode detail = objectMapper.readTree(created.getResponse().getContentAsString());
        ObjectNode overCapacity = saveBodyFromDetail(detail);
        ((ArrayNode) overCapacity.get("groups").get(0).get("students"))
                .addObject()
                .put("studentId", detail.get("unassignedDemand").get(0).get("studentId").asLong())
                .putNull("obs");

        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", detail.get("id").asLong())
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overCapacity.toString()))
                .andExpect(status().isBadRequest());

        ObjectNode tooManyGroups = saveBodyFromDetail(detail);
        ArrayNode groups = (ArrayNode) tooManyGroups.get("groups");
        ObjectNode extraGroup = ((ObjectNode) groups.get(0)).deepCopy();
        extraGroup.remove("id");
        extraGroup.put("grupo", "CO43A");
        ArrayNode extraStudents = (ArrayNode) extraGroup.get("students");
        extraStudents.removeAll();
        extraStudents.addObject()
                .put("studentId", detail.get("unassignedDemand").get(0).get("studentId").asLong())
                .putNull("obs");
        groups.add(extraGroup);
        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", detail.get("id").asLong())
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tooManyGroups.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("regenerating a plan that already has unassigned demand rebuilds the same pairs")
    void regenerateRebuildsUnassignedDemand() throws Exception {
        String thirdToken = createStudentAndLogin(
                "trimestral-student8@uam.mx", "2123999108", "Grace", "Hopper", "Murray");
        long surveyId = createActiveSurvey("26O");
        submitEnrollResponse(surveyId, studentToken(), "I", ueaId);
        submitEnrollResponse(surveyId, login(STUDENT2_EMAIL), "I", ueaId);
        submitEnrollResponse(surveyId, thirdToken, "I", ueaId);
        closeSurvey(surveyId);
        createAnnualPlanWithCapacity(2026, "1", "2");

        MvcResult created = mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unassignedDemand.length()").value(1))
                .andReturn();
        long planId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        // La demanda regenerada repite el par (plan, UEA, alumno) que ya está en la tabla.
        mockMvc.perform(post("/api/trimestral-plans/{id}/regenerate", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unassignedDemand.length()").value(1))
                .andExpect(jsonPath("$.unassignedDemand[0].reason").value("GROUP_LIMIT_REACHED"));
    }

    @Test
    @DisplayName("annual wildcard groups open as many capacity groups as demand requires")
    void annualWildcardGroups() throws Exception {
        String thirdToken = createStudentAndLogin(
                "trimestral-student4@uam.mx", "2123999104", "Diana", "Ross", "King");
        long surveyId = createActiveSurvey("26O");
        submitEnrollResponse(surveyId, studentToken(), "I", ueaId);
        submitEnrollResponse(surveyId, login(STUDENT2_EMAIL), "I", ueaId);
        submitEnrollResponse(surveyId, thirdToken, "I", ueaId);
        closeSurvey(surveyId);
        createAnnualPlanWithCapacity(2026, "*", "2");

        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groups.length()").value(2))
                .andExpect(jsonPath("$.groups[0].students.length()").value(2))
                .andExpect(jsonPath("$.groups[1].grupo").value("CO43A"))
                .andExpect(jsonPath("$.groups[1].students.length()").value(1))
                .andExpect(jsonPath("$.warnings[?(@.code=='CUPO_EXCEEDED')]").doesNotExist());
    }

    @Test
    @DisplayName("fills a second authorized numeric group after the first reaches capacity")
    void annualNumericGroupsFillSequentially() throws Exception {
        String thirdToken = createStudentAndLogin(
                "trimestral-student5@uam.mx", "2123999105", "Emmy", "Noether", "Smith");
        long surveyId = createActiveSurvey("26O");
        submitEnrollResponse(surveyId, studentToken(), "I", ueaId);
        submitEnrollResponse(surveyId, login(STUDENT2_EMAIL), "I", ueaId);
        submitEnrollResponse(surveyId, thirdToken, "I", ueaId);
        closeSurvey(surveyId);
        createAnnualPlanWithCapacity(2026, "2", "2");

        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groups.length()").value(2))
                .andExpect(jsonPath("$.groups[0].grupo").value("CO43"))
                .andExpect(jsonPath("$.groups[0].students.length()").value(2))
                .andExpect(jsonPath("$.groups[1].grupo").value("CO43A"))
                .andExpect(jsonPath("$.groups[1].students.length()").value(1))
                .andExpect(jsonPath("$.warnings[?(@.code=='CUPO_EXCEEDED')]").doesNotExist());
    }

    @Test
    @DisplayName("research group limit preserves global surname priority across academic-term bases")
    void researchGroupLimitUsesGlobalPriorityAcrossBases() throws Exception {
        long researchUeaId =
                createUea("2156099", "PROYECTO DE INVESTIGACIÓN", FormationType.INVESTIGACION);
        String anaToken = createStudentAndLogin(
                "trimestral-priority@uam.mx", "2123999000", "Ana", "Aguirre", "Lopez");
        long surveyId = createActiveSurvey("26O");
        submitEnrollResponse(surveyId, anaToken, "I", researchUeaId);
        submitEnrollResponse(surveyId, login(STUDENT2_EMAIL), "II", researchUeaId);
        submitEnrollResponse(surveyId, studentToken(), "I", researchUeaId);
        closeSurvey(surveyId);
        createAnnualPlanForUeaWithCapacity(2026, researchUeaId, "2", "1");

        mockMvc.perform(post("/api/trimestral-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveyId\":" + surveyId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groups.length()").value(2))
                .andExpect(jsonPath("$.groups[0].grupo").value("CO43"))
                .andExpect(jsonPath("$.groups[0].students[0].enrollmentId").value("2123999000"))
                .andExpect(jsonPath("$.groups[1].grupo").value("CP43"))
                .andExpect(jsonPath("$.groups[1].students[0].studentId").value(student2Id))
                .andExpect(jsonPath("$.unassignedDemand.length()").value(1))
                .andExpect(jsonPath("$.unassignedDemand[0].studentId").value(studentId))
                .andExpect(jsonPath("$.unassignedDemand[0].reason").value("GROUP_LIMIT_REACHED"));
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
    @DisplayName("reopen survey marks even a TERMINADA plan outdated without changing its content")
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
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINADA"))
                .andExpect(jsonPath("$.groups.length()").value(1))
                .andExpect(jsonPath("$.outdated").value(true))
                .andExpect(jsonPath("$.outdatedReasons[0]").value("SURVEY_REOPENED"))
                .andExpect(jsonPath("$.prerequisites.surveyClosed").value(false));

        ObjectNode blockedSave = saveBodyFromDetail(getPlanDetail(planId));
        mockMvc.perform(put("/api/trimestral-plans/{id}/groups", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockedSave.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SURVEY_NOT_CLOSED"));

        mockMvc.perform(post("/api/trimestral-plans/{id}/regenerate", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SURVEY_NOT_CLOSED"));

        mockMvc.perform(get("/api/trimestral-plans/{id}/export", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SURVEY_NOT_CLOSED"));

        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BORRADOR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SURVEY_NOT_CLOSED"));

        closeSurvey(surveyId);
        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BORRADOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outdated").value(true))
                .andExpect(jsonPath("$.outdatedReasons[0]").value("SURVEY_REOPENED"))
                .andExpect(jsonPath("$.prerequisites.surveyClosed").value(true));

        mockMvc.perform(post("/api/trimestral-plans/{id}/regenerate", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outdated").value(false))
                .andExpect(jsonPath("$.outdatedReasons").isEmpty());
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
        ArrayNode students = (ArrayNode) saveBody.get("groups").get(0).get("students");
        students.removeAll();
        students.addObject().put("studentId", foreignStudentId).putNull("obs");

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
    @DisplayName("HU-60: export BORRADOR plan is allowed")
    void exportBorradorPlan() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        mockMvc.perform(get("/api/trimestral-plans/{id}/export", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition", "attachment; filename=\"PCYTI 26O.xlsx\""));
    }

    @Test
    @DisplayName("HU-60: export TERMINADA plan is allowed")
    void exportTerminadaPlan() throws Exception {
        long surveyId = createClosedSurveyWithResponse("26O");
        createAnnualPlanWithCupo(2026, "25");
        long planId = generatePlan(surveyId);

        markTerminada(planId);

        mockMvc.perform(get("/api/trimestral-plans/{id}/export", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition", "attachment; filename=\"PCYTI 26O.xlsx\""));

        mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportedAt").isNotEmpty());
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
        markTerminada(planId);

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
        g.putArray("professorIds");
        ArrayNode schedule = g.putArray("schedule");
        for (String day : new String[] {"LUN", "MAR", "MIE", "JUE", "VIE"}) {
            ObjectNode slot = schedule.addObject();
            slot.put("day", day);
            slot.putNull("start");
            slot.putNull("end");
            slot.put("lab", false);
        }
        ArrayNode students = g.putArray("students");
        ObjectNode s1 = students.addObject();
        s1.put("studentId", studentId);
        s1.putNull("obs");
        ObjectNode s2 = students.addObject();
        s2.put("studentId", student2Id);
        s2.put("obs", "Maestría Física");
        return body;
    }

    private void markTerminada(long planId) throws Exception {
        mockMvc.perform(patch("/api/trimestral-plans/{id}/status", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk());
    }

    private ObjectNode saveBodyFromDetail(JsonNode detail) {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode groups = body.putArray("groups");
        for (JsonNode source : detail.get("groups")) {
            ObjectNode target = groups.addObject();
            target.put("id", source.get("id").asLong());
            target.put("ueaId", source.get("ueaId").asLong());
            if (source.get("grupo").isNull()) target.putNull("grupo");
            else target.put("grupo", source.get("grupo").asText());
            if (source.get("cupo").isNull()) target.putNull("cupo");
            else target.put("cupo", source.get("cupo").asText());
            ArrayNode professorIds = target.putArray("professorIds");
            for (JsonNode professor : source.get("professors")) {
                professorIds.add(professor.get("professorId").asLong());
            }
            target.set("schedule", source.get("schedule").deepCopy());
            ArrayNode students = target.putArray("students");
            for (JsonNode student : source.get("students")) {
                ObjectNode member = students.addObject();
                member.put("studentId", student.get("studentId").asLong());
                if (student.get("obs").isNull()) member.putNull("obs");
                else member.put("obs", student.get("obs").asText());
            }
        }
        return body;
    }

    private JsonNode getPlanDetail(long planId) throws Exception {
        MvcResult detail = mockMvc.perform(get("/api/trimestral-plans/{id}", planId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(detail.getResponse().getContentAsString());
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
        submitEnrollResponse(surveyId, studentToken(), "I", ueaId);
    }

    private void submitEnrollResponse(long surveyId, String token, String academicTerm, long... ueaIds)
            throws Exception {
        ObjectNode submit = objectMapper.createObjectNode();
        submit.put("academicTerm", academicTerm);
        submit.put("mode", "ENROLL_UEAS");
        ArrayNode selected = submit.putArray("ueaIds");
        for (long selectedUeaId : ueaIds) {
            selected.add(selectedUeaId);
        }
        mockMvc.perform(post("/api/enrollment-surveys/{id}/responses", surveyId)
                        .header("Authorization", "Bearer " + token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submit.toString()))
                .andExpect(status().isOk());
    }

    private Professor createProfessor(String nemp, String firstName, String lastName, String email) {
        User user = userRepository.save(new User(
                email, new BCryptPasswordEncoder().encode(PASSWORD), RoleType.PROFESSOR, programId));
        return professorRepository.save(internoProfessor(
                nemp, user.getId(), programId, minimalProfessorPersonalData(firstName, lastName)));
    }

    private String createStudentAndLogin(
            String email, String enrollmentId, String firstName, String firstLastName, String secondLastName)
            throws Exception {
        User user = userRepository.save(new User(
                email, new BCryptPasswordEncoder().encode(PASSWORD), RoleType.STUDENT, programId));
        studentRepository.save(new Student(
                enrollmentId,
                user.getId(),
                programId,
                null,
                new PersonalData(
                        firstName, firstLastName, secondLastName, "Mexicana", null, "5554820000", null),
                sampleAcademicInformation()));
        return login(email);
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
        createAnnualPlanWithCapacity(year, "*", cupoO);
    }

    private void createAnnualPlanWithCapacity(int year, String gruposO, String cupoO) throws Exception {
        createAnnualPlanDraftWithCapacity(year, gruposO, cupoO);

        mockMvc.perform(patch("/api/annual-plans/{year}/status", year)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk());
    }

    private void createAnnualPlanDraftWithCapacity(int year, String gruposO, String cupoO) throws Exception {
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
        if (gruposO == null) {
            entry.putNull("gruposO");
        } else {
            entry.put("gruposO", gruposO);
        }
        entry.put("cupoO", cupoO);
        entry.putObject("marks");

        mockMvc.perform(put("/api/annual-plans/{year}/entries", year)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isOk());
    }

    private void createAnnualPlanForUeaWithCapacity(
            int year, long targetUeaId, String gruposO, String cupoO) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/annual-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":" + year + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        JsonNode targetEntry = null;
        for (JsonNode entry : body.get("entries")) {
            if (entry.get("ueaId").asLong() == targetUeaId) {
                targetEntry = entry;
                break;
            }
        }
        assertThat(targetEntry).isNotNull();

        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode entry = payload.putArray("entries").addObject();
        entry.put("id", targetEntry.get("id").asLong());
        entry.put("gruposO", gruposO);
        entry.put("cupoO", cupoO);
        entry.putObject("marks");
        mockMvc.perform(put("/api/annual-plans/{year}/entries", year)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/annual-plans/{year}/status", year)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk());
    }

    private long createUea(String clave, String nombre) throws Exception {
        return createUea(clave, nombre, FormationType.BASICA);
    }

    private long createUea(String clave, String nombre, FormationType formationType) throws Exception {
        RegisterUeaRequest request = new RegisterUeaRequest(
                clave,
                nombre,
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                formationType,
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

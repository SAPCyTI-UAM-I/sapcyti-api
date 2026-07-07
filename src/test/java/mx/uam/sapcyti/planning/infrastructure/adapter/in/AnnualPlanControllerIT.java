package mx.uam.sapcyti.planning.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import java.math.BigDecimal;
import java.util.List;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import mx.uam.sapcyti.offering.infrastructure.adapter.out.repository.SpringDataUeaRepository;
import mx.uam.sapcyti.planning.testutil.PlanningTestXlsx;
import mx.uam.sapcyti.planning.testutil.PlanningTestXlsx.UeaRow;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class AnnualPlanControllerIT {

    private static final String COORDINATOR_EMAIL = "plan-coordinator@uam.mx";
    private static final String STUDENT_EMAIL = "plan-student@uam.mx";
    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataUeaRepository ueaRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    private Long programId;

    @BeforeEach
    void seed() {
        ueaRepository.deleteAll();
        userRepository.deleteAll();

        GraduateProgram program = programAdapter.save(
                new GraduateProgram("PCyTI Planning " + System.nanoTime(), "CBI"));
        programId = program.getId();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(PASSWORD);
        userRepository.save(new User(COORDINATOR_EMAIL, hash, RoleType.COORDINATOR, programId));
        userRepository.save(new User(STUDENT_EMAIL, hash, RoleType.STUDENT, programId));

        ueaRepository.save(sampleUea("2156041", "MÉTODOS MATEMÁTICOS"));
        ueaRepository.save(sampleUea("2156099", "UEA BAJA"));
    }

    @Test
    @DisplayName("format check returns differences without persisting file")
    void checkFormat() throws Exception {
        byte[] content = PlanningTestXlsx.buildSampleFile(
                2027,
                List.of(new UeaRow("9999999", "EXTRA"), new UeaRow("2156041", "MÉTODOS MATEMÁTICOS")));

        mockMvc.perform(multipart("/api/annual-plans/check")
                        .file(new MockMultipartFile(
                                "file", "planeacion.xlsx", "application/vnd.ms-excel", content))
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missingInCatalog[0].clave").value("9999999"))
                .andExpect(jsonPath("$.missingInFile[0].clave").value("2156099"));
    }

    @Test
    @DisplayName("invalid file format returns FILE_FORMAT_INVALID")
    void invalidFileFormat() throws Exception {
        mockMvc.perform(multipart("/api/annual-plans/check")
                        .file(new MockMultipartFile("file", "bad.csv", "text/csv", "a,b".getBytes()))
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("FILE_FORMAT_INVALID"));
    }

    @Test
    @DisplayName("coordinator creates annual plan in BORRADOR")
    void createPlan() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/annual-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2027}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BORRADOR"))
                .andExpect(jsonPath("$.terms[0]").value("27-I"))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("entries").get(0).get("clave").asText()).isNotBlank();
    }

    @Test
    @DisplayName("duplicate year returns ANNUAL_PLAN_ALREADY_EXISTS")
    void duplicateYear() throws Exception {
        createPlan2027();
        mockMvc.perform(post("/api/annual-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2027}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ANNUAL_PLAN_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("list and get annual plans")
    void listAndGet() throws Exception {
        createPlan2027();
        mockMvc.perform(get("/api/annual-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2027))
                .andExpect(jsonPath("$[0].status").value("BORRADOR"));

        mockMvc.perform(get("/api/annual-plans/2027")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].posicion").doesNotExist())
                .andExpect(jsonPath("$.entries[0].clave").exists());
    }

    @Test
    @DisplayName("save entries in BORRADOR")
    void saveEntries() throws Exception {
        createPlan2027();
        long entryId = firstEntryId(2027);

        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode entries = payload.putArray("entries");
        ObjectNode entry = entries.addObject();
        entry.put("id", entryId);
        entry.put("gruposI", "2");
        entry.put("cupoI", "15");
        entry.put("gruposP", "*");
        entry.put("cupoP", "*");
        ObjectNode marks = entry.putObject("marks");
        marks.put("P_FIS", "X");
        marks.put("PCYTI", "O"); // ignored: PCYTI is derived from the UEA (OBLIGATORIA -> X)

        mockMvc.perform(put("/api/annual-plans/2027/entries")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].gruposI").value("2"))
                .andExpect(jsonPath("$.entries[0].marks.P_FIS").value("X"))
                .andExpect(jsonPath("$.entries[0].marks.PCYTI").value("X"));
    }

    @Test
    @DisplayName("invalid cell rejects whole save")
    void invalidSave() throws Exception {
        createPlan2027();
        long entryId = firstEntryId(2027);

        mockMvc.perform(put("/api/annual-plans/2027/entries")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[{"id":%d,"cupoI":"abc"}]}
                                """.formatted(entryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("save rejected when plan is TERMINADA")
    void saveNotEditable() throws Exception {
        createPlan2027();
        terminatePlan(2027);
        long entryId = firstEntryId(2027);

        mockMvc.perform(put("/api/annual-plans/2027/entries")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[{"id":%d,"gruposI":"1"}]}
                                """.formatted(entryId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PLAN_NOT_EDITABLE"));
    }

    @Test
    @DisplayName("export annual plan as xlsx")
    void exportPlan() throws Exception {
        createPlan2027();
        mockMvc.perform(get("/api/annual-plans/2027/export")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("Planeacion PCyTI 2027.xlsx")));
    }

    @Test
    @DisplayName("status transitions enforce adjacency")
    void statusCycle() throws Exception {
        createPlan2027();

        mockMvc.perform(patch("/api/annual-plans/2027/status")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVADA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(patch("/api/annual-plans/2027/status")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINADA"));

        mockMvc.perform(patch("/api/annual-plans/2027/status")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVADA"));
    }

    @Test
    @DisplayName("non-coordinator cannot access annual planning")
    void forbiddenForStudent() throws Exception {
        mockMvc.perform(get("/api/annual-plans")
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isForbidden());
    }

    private void createPlan2027() throws Exception {
        mockMvc.perform(post("/api/annual-plans")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2027}"))
                .andExpect(status().isCreated());
    }

    private void terminatePlan(int year) throws Exception {
        mockMvc.perform(patch("/api/annual-plans/" + year + "/status")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINADA\"}"))
                .andExpect(status().isOk());
    }

    private long firstEntryId(int year) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/annual-plans/" + year)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("entries")
                .get(0)
                .get("id")
                .asLong();
    }

    private UEA sampleUea(String clave, String nombre) {
        return UEA.create(
                programId,
                clave,
                nombre,
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                FormationType.BASICA,
                9);
    }

    private String coordinatorToken() throws Exception {
        return loginToken(COORDINATOR_EMAIL);
    }

    private String studentToken() throws Exception {
        return loginToken(STUDENT_EMAIL);
    }

    private String loginToken(String email) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(PASSWORD)
                .build();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}

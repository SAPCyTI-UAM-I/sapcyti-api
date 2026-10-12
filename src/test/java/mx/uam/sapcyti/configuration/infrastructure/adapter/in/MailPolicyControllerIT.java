package mx.uam.sapcyti.configuration.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SPEC-041 HTTP contract for the global mail policy.
 * Flyway is off in this profile, so each test installs the same initial row the migration would.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class MailPolicyControllerIT {

    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    private Long program1Id;
    private Long program2Id;
    private Long coordinator1Id;
    private Long coordinator2Id;
    private String coordinator1Token;
    private String coordinator2Token;
    private String studentToken;
    private String adminToken;
    private String assistantToken;
    private String professorToken;

    @BeforeEach
    void setUp() throws Exception {
        resetPolicy();
        String suffix = Long.toString(System.nanoTime());
        GraduateProgram program1 = programAdapter.save(
                new GraduateProgram("Mail policy program 1 " + suffix, "CBI"));
        GraduateProgram program2 = programAdapter.save(
                new GraduateProgram("Mail policy program 2 " + suffix, "CBS"));
        program1Id = program1.getId();
        program2Id = program2.getId();

        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        User coordinator1 = userRepository.save(
                new User("mail-policy-coordinator-1-" + suffix + "@uam.mx", hash, RoleType.COORDINATOR, program1Id));
        User coordinator2 = userRepository.save(
                new User("mail-policy-coordinator-2-" + suffix + "@uam.mx", hash, RoleType.COORDINATOR, program2Id));
        User student = userRepository.save(
                new User("mail-policy-student-" + suffix + "@uam.mx", hash, RoleType.STUDENT, program1Id));
        User admin = userRepository.save(
                new User("mail-policy-admin-" + suffix + "@uam.mx", hash, RoleType.SYSTEM_ADMIN, null));
        User assistant = userRepository.save(
                new User("mail-policy-assistant-" + suffix + "@uam.mx", hash, RoleType.ASSISTANT, program1Id));
        User professor = userRepository.save(
                new User("mail-policy-professor-" + suffix + "@uam.mx", hash, RoleType.PROFESSOR, program1Id));
        coordinator1Id = coordinator1.getId();
        coordinator2Id = coordinator2.getId();
        coordinator1Token = login(coordinator1.getEmail());
        coordinator2Token = login(coordinator2.getEmail());
        studentToken = login(student.getEmail());
        adminToken = login(admin.getEmail());
        assistantToken = login(assistant.getEmail());
        professorToken = login(professor.getEmail());
    }

    @AfterEach
    void tearDown() {
        resetPolicy();
    }

    @Test
    @DisplayName("Scenario: Query the initial global policy and the same row from two programs")
    void initialPolicyIsTheSameRowForTwoPrograms() throws Exception {
        MvcResult withHeader = mockMvc.perform(get("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, program1Id))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().exists(TenantFilter.HEADER_REQUEST_ID))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
                .andReturn();
        MvcResult withoutHeader = mockMvc.perform(get("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator2Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();

        String first = withHeader.getResponse().getContentAsString();
        String second = withoutHeader.getResponse().getContentAsString();
        assertThat(first).isEqualTo(second);
        assertInitialJson(first);
    }

    @Test
    @DisplayName("Scenario: A valid tenant header does not scope the policy")
    void tenantHeaderDoesNotSelectAnotherRow() throws Exception {
        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator2Token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, program2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"TEST\",\"version\":0,\"graduateProgramId\":" + program2Id + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("body"));
        assertThat(storedMode()).isEqualTo("DISABLED");
        assertThat(storedVersion()).isZero();

        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator2Token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, program2Id)
                        .contentType("application/json;charset=UTF-8")
                        .content("{\"mode\":\"TEST\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("TEST"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(get("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .header(TenantFilter.HEADER_GRADUATE_ID, program1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("TEST"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.updatedByUserId").value(coordinator2Id));
    }

    @Test
    @DisplayName("Scenario: Same mode and current version is a no-op; an obsolete version is a conflict")
    void noOpKeepsVersionAndObsoleteVersionConflicts() throws Exception {
        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"TEST\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.mode").value("TEST"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.updatedByUserId").value(coordinator1Id))
                .andExpect(jsonPath("$.updatedAt").value(org.hamcrest.Matchers.matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")));
        assertThat(auditCount()).isEqualTo(1);

        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"TEST\",\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("TEST"))
                .andExpect(jsonPath("$.version").value(1));
        assertThat(storedVersion()).isEqualTo(1);
        assertThat(auditCount()).isEqualTo(1);

        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"ENABLED\",\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error").value("VERSION_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Mail policy changed; reload before saving"))
                .andExpect(jsonPath("$.details.currentVersion").value(1))
                .andExpect(jsonPath("$.details.fieldErrors").doesNotExist());
        assertThat(storedMode()).isEqualTo("TEST");
        assertThat(storedVersion()).isEqualTo(1);
        assertThat(auditCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario: Only a coordinator can read or change the policy")
    void otherRolesAreForbiddenBeforeValidationAndMissingBearerIsUnauthorized() throws Exception {
        for (String token : List.of(studentToken, adminToken, assistantToken, professorToken)) {
            mockMvc.perform(get("/api/mail-policy")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.details").doesNotExist());
            mockMvc.perform(put("/api/mail-policy")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.details").doesNotExist());
        }
        mockMvc.perform(get("/api/mail-policy"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").doesNotExist());
        mockMvc.perform(put("/api/mail-policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/mail-policy")
                        .header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        assertThat(storedMode()).isEqualTo("DISABLED");
        assertThat(storedVersion()).isZero();
        assertThat(auditCount()).isZero();
    }

    @Test
    @DisplayName("Scenario: Body, enum, decimal version, unknown query and content type")
    void validationAndUnsupportedMediaType() throws Exception {
        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"TEST\",\"version\":0,\"to\":\"someone@uam.mx\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("body"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("INVALID_VALUE"))
                .andExpect(jsonPath("$.details.fieldErrors[0].message", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("to"))));

        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"De Prueba\",\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid mail mode"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("mode"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("INVALID_ENUM"))
                .andExpect(jsonPath("$.details.fieldErrors[0].message").value("Use DISABLED, TEST or ENABLED"));

        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"TEST\",\"version\":1.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("version"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("INVALID_TYPE"));

        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"TEST\",\"version\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("version"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("OUT_OF_RANGE"));

        mockMvc.perform(get("/api/mail-policy?graduateProgramId={id}", program1Id)
                        .header("Authorization", "Bearer " + coordinator1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("query"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("INVALID_VALUE"));

        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"mode\":\"TEST\",\"version\":0}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.details").doesNotExist());

        assertThat(storedMode()).isEqualTo("DISABLED");
        assertThat(storedVersion()).isZero();
        assertThat(auditCount()).isZero();
    }

    @Test
    @DisplayName("Scenario: These routes publish details; a legacy error does not")
    void envelopeOmitsDetailsOnLegacyErrors() throws Exception {
        MvcResult success = mockMvc.perform(get("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token))
                .andExpect(status().isOk())
                .andReturn();
        assertInitialJson(success.getResponse().getContentAsString());

        mockMvc.perform(put("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"TEST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.details.fieldErrors[0].field").value("version"))
                .andExpect(jsonPath("$.details.fieldErrors[0].code").value("REQUIRED"));

        mockMvc.perform(get("/api/programs/999999999")
                        .header("Authorization", "Bearer " + coordinator1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Graduate program not found"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("Scenario: A missing policy row does not assume a mode")
    void missingRowIsUnavailable() throws Exception {
        jdbc.update("DELETE FROM mail_policy_audit_entries");
        jdbc.update("DELETE FROM mail_policy");

        mockMvc.perform(get("/api/mail-policy")
                        .header("Authorization", "Bearer " + coordinator1Token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error").value("MAIL_POLICY_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Mail policy is temporarily unavailable"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.mode").doesNotExist());
    }

    private void assertInitialJson(String json) throws Exception {
        JsonNode body = objectMapper.readTree(json);
        assertThat(body.size()).isEqualTo(4);
        assertThat(body.get("mode").asText()).isEqualTo("DISABLED");
        assertThat(body.get("version").asInt()).isZero();
        assertThat(body.has("updatedAt")).isTrue();
        assertThat(body.get("updatedAt").isNull()).isTrue();
        assertThat(body.has("updatedByUserId")).isTrue();
        assertThat(body.get("updatedByUserId").isNull()).isTrue();
    }

    private String login(String email) throws Exception {
        LoginRequest request = LoginRequest.builder().email(email).password(PASSWORD).build();
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private void resetPolicy() {
        jdbc.update("DELETE FROM mail_policy_audit_entries");
        int updated = jdbc.update("""
                UPDATE mail_policy
                SET mode = 'DISABLED', version = 0, updated_at = NULL, updated_by_user_id = NULL
                WHERE id = 1
                """);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO mail_policy (id, mode, version, updated_at, updated_by_user_id)
                    VALUES (1, 'DISABLED', 0, NULL, NULL)
                    """);
        }
    }

    private String storedMode() {
        return jdbc.queryForObject("SELECT mode FROM mail_policy WHERE id = 1", String.class);
    }

    private int storedVersion() {
        return jdbc.queryForObject("SELECT version FROM mail_policy WHERE id = 1", Integer.class);
    }

    private int auditCount() {
        Number count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM mail_policy_audit_entries", Number.class);
        return count == null ? 0 : count.intValue();
    }
}

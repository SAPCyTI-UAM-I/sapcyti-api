package mx.uam.sapcyti.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * SPEC-039 — prod surface: Actuator health only; OpenAPI/Swagger not publicly usable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"prod", "integration-test"})
class ProdActuatorOpenApiIT {

    @DynamicPropertySource
    static void prodSecrets(DynamicPropertyRegistry registry) {
        registry.add("jwt.private-key", () -> readPem("jwt/dev-private.pem"));
        registry.add("jwt.public-key", () -> readPem("jwt/dev-public.pem"));
        registry.add("app.email.provider", () -> "smtp");
        // ponytail: CI has no SMTP; this IT checks the health surface, not mail reachability
        registry.add("management.health.mail.enabled", () -> "false");
    }

    private static String readPem(String classpathLocation) {
        try {
            return new String(
                    new ClassPathResource(classpathLocation).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read " + classpathLocation, e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("prod: GET /actuator/health returns 200 without details")
    void healthIsReachableWithoutDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @DisplayName("prod: GET /actuator/info is not exposed")
    void infoIsNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("prod: GET /actuator/prometheus is not exposed")
    void prometheusIsNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("prod: OpenAPI/Swagger paths are not usable anonymously")
    void docsPathsAreNotPublic() throws Exception {
        assertNotUsableAnonymously("/api-docs");
        assertNotUsableAnonymously("/docs");
        assertNotUsableAnonymously("/swagger-ui/index.html");
    }

    private void assertNotUsableAnonymously(String path) throws Exception {
        ResultActions actions = mockMvc.perform(get(path));
        int status = actions.andReturn().getResponse().getStatus();
        assertThat(status)
                .as("anonymous GET %s should be 401 or 404 in prod", path)
                .isIn(401, 404);
    }
}

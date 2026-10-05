package mx.uam.sapcyti.identity.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * SPEC-038 — prod Flyway locations exclude migration-dev; JWT inline keys allow context to start.
 */
@SpringBootTest
@ActiveProfiles({"prod", "integration-test"})
class ProdAuthFlywayConfigIT {

    @DynamicPropertySource
    static void prodJwtAndSmtp(DynamicPropertyRegistry registry) throws Exception {
        registry.add("jwt.private-key", () -> readPem("jwt/dev-private.pem"));
        registry.add("jwt.public-key", () -> readPem("jwt/dev-public.pem"));
        // Avoid Resend RestClient fail-fast in this config assertion IT
        registry.add("app.email.provider", () -> "smtp");
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
    private Environment environment;

    @Test
    @DisplayName("prod flyway.locations exclude migration-dev (V7/V8/V18)")
    void prodFlywayLocationsExcludeDevSeeds() {
        String locations = environment.getProperty("spring.flyway.locations");
        assertThat(locations).isEqualTo("classpath:db/migration");
        assertThat(locations).doesNotContain("migration-dev");
    }

    @Test
    @DisplayName("prod requires inline JWT keys")
    void prodRequiresInlineJwtKeys() {
        assertThat(environment.getProperty("jwt.require-inline-keys", Boolean.class)).isTrue();
    }
}

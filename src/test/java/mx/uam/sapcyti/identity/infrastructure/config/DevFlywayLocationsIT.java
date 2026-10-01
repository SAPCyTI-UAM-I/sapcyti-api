package mx.uam.sapcyti.identity.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * SPEC-038 — non-prod profiles keep migration-dev (V7/V8/V18) on the Flyway classpath.
 */
@SpringBootTest
@ActiveProfiles("integration-test")
class DevFlywayLocationsIT {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("default flyway.locations include migration-dev seeds")
    void defaultLocationsIncludeMigrationDev() {
        String locations = environment.getProperty("spring.flyway.locations");
        assertThat(locations).contains("classpath:db/migration");
        assertThat(locations).contains("classpath:db/migration-dev");
    }
}

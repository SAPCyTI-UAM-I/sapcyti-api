package mx.uam.sapcyti.identity.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.uam.sapcyti.SapcytiApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * SPEC-038 — prod profile without JWT_* PEM fails to start (no classpath key fallback).
 */
class JwtProdContextFailFastIT {

    @Test
    @DisplayName("prod context fails fast when JWT PEM env keys are missing")
    void prodContextFailsWithoutJwtPem() {
        assertThatThrownBy(() -> {
            try (ConfigurableApplicationContext ignored = SpringApplication.run(
                    SapcytiApplication.class,
                    "--spring.profiles.active=prod,test",
                    "--spring.main.web-application-type=none",
                    "--jwt.require-inline-keys=true",
                    "--jwt.private-key=",
                    "--jwt.public-key=",
                    "--jwt.private-key-path=classpath:jwt/dev-private.pem",
                    "--jwt.public-key-path=classpath:jwt/dev-public.pem",
                    "--app.email.provider=smtp"
            )) {
                // should not reach — classpath paths must not satisfy require-inline-keys
            }
        }).hasRootCauseInstanceOf(IllegalStateException.class)
                .rootCause()
                .hasMessageContaining("inline PEM");
    }
}

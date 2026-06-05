package mx.uam.sapcyti.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(String baseUrl, int ttlMinutes) {
}

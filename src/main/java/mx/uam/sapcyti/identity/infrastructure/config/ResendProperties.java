package mx.uam.sapcyti.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email.resend")
public record ResendProperties(
        String apiKey,
        String from,
        String baseUrl
) {
    public ResendProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.resend.com";
        }
        if (from == null || from.isBlank()) {
            from = "SAPCyTI <noreply@uam.mx>";
        }
    }
}

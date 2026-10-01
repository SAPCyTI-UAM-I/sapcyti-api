package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.identity.domain.port.out.EmailPort;
import mx.uam.sapcyti.identity.infrastructure.config.PasswordResetProperties;
import mx.uam.sapcyti.identity.infrastructure.config.ResendProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Production email delivery via Resend HTTPS API (SPEC-038 / closes SPEC-015 pending).
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
@RequiredArgsConstructor
@Slf4j
public class ResendEmailAdapter implements EmailPort {

    private final RestClient resendRestClient;
    private final SpringTemplateEngine templateEngine;
    private final PasswordResetProperties passwordResetProperties;
    private final ResendProperties resendProperties;

    @Override
    public void sendPasswordReset(String toEmail, String rawToken, Locale locale) {
        Locale effectiveLocale = locale != null ? locale : Locale.forLanguageTag("es");
        String language = effectiveLocale.getLanguage().startsWith("en") ? "en" : "es";
        String templateName = "email/password-reset_" + language;
        String resetUrl = passwordResetProperties.baseUrl() + "/auth/reset-password?token=" + rawToken;

        Context context = new Context(effectiveLocale);
        context.setVariable("resetUrl", resetUrl);

        String htmlBody = templateEngine.process(templateName, context);
        String subject = language.equals("en")
                ? "SAPCyTI — Password recovery"
                : "SAPCyTI — Recuperación de contraseña";

        Map<String, Object> body = Map.of(
                "from", resendProperties.from(),
                "to", List.of(toEmail),
                "subject", subject,
                "html", htmlBody
        );

        try {
            resendRestClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Password reset email sent via Resend to {}", toEmail);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Failed to send password reset email via Resend (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send password reset email via Resend", e);
        }
    }
}

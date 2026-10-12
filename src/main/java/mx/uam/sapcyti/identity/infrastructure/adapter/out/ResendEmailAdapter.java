package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.configuration.domain.model.MailDispatchCommand;
import mx.uam.sapcyti.configuration.domain.model.MailEnvelope;
import mx.uam.sapcyti.configuration.domain.port.in.MailDispatchPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
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
 *
 * <p>{@code POST /emails} runs only inside the dispatch callback, with To, Cc and Bcc
 * taken from the envelope the gate resolved for the mode it just read.
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
    private final MailDispatchPort mailDispatch;

    @Override
    public void sendPasswordReset(String toEmail, String rawToken, Locale locale, RoleType role) {
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

        MailEnvelope intended = new MailEnvelope(List.of(toEmail), List.of(), List.of());
        mailDispatch.dispatch(
                new MailDispatchCommand(PasswordResetAudience.of(role), intended),
                envelope -> deliver(envelope, subject, htmlBody));
    }

    private void deliver(MailEnvelope envelope, String subject, String htmlBody) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", resendProperties.from());
        body.put("to", envelope.to());
        if (!envelope.cc().isEmpty()) {
            body.put("cc", envelope.cc());
        }
        if (!envelope.bcc().isEmpty()) {
            body.put("bcc", envelope.bcc());
        }
        body.put("subject", subject);
        body.put("html", htmlBody);

        try {
            resendRestClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Password reset email accepted via Resend for {}", envelope.to());
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Failed to send password reset email via Resend (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to send password reset email via Resend", e);
        }
    }
}

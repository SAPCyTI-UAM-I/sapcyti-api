package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.configuration.domain.model.MailDispatchCommand;
import mx.uam.sapcyti.configuration.domain.model.MailEnvelope;
import mx.uam.sapcyti.configuration.domain.port.in.MailDispatchPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.port.out.EmailPort;
import mx.uam.sapcyti.identity.infrastructure.config.PasswordResetProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * SMTP email delivery for non-prod profiles (MailHog / local SMTP). Prod uses {@link ResendEmailAdapter}.
 *
 * <p>The provider is reached only from the dispatch callback, using the envelope
 * the gate just resolved. This adapter does not keep a mode of its own.
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final PasswordResetProperties passwordResetProperties;
    private final MailDispatchPort mailDispatch;

    @Value("${spring.mail.username:noreply@uam.mx}")
    private String fromAddress;

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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(addresses(envelope.to()));
            if (!envelope.cc().isEmpty()) {
                helper.setCc(addresses(envelope.cc()));
            }
            if (!envelope.bcc().isEmpty()) {
                helper.setBcc(addresses(envelope.bcc()));
            }
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.debug("Password reset email accepted for {}", envelope.to());
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send password reset email", e);
        }
    }

    private static String[] addresses(List<String> recipients) {
        return recipients.toArray(String[]::new);
    }
}

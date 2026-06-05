package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.identity.domain.port.out.EmailPort;
import mx.uam.sapcyti.identity.infrastructure.config.PasswordResetProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final PasswordResetProperties passwordResetProperties;

    @Value("${spring.mail.username:noreply@uam.mx}")
    private String fromAddress;

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

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.debug("Password reset email sent to {}", toEmail);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send password reset email", e);
        }
    }
}

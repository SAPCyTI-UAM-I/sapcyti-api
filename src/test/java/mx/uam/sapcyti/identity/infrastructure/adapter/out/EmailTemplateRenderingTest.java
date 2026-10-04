package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class EmailTemplateRenderingTest {

    private SpringTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Test
    @DisplayName("password-reset_es renders correctly with clean punctuation and institutional name")
    void testPasswordResetEsRendering() {
        Context context = new Context(Locale.forLanguageTag("es"));
        context.setVariable("resetUrl", "https://sapcyti.site/auth/reset-password?token=abc-123");
        context.setVariable("userName", "Roberto Juárez");
        context.setVariable("expirationMinutes", 30);

        String html = templateEngine.process("email/password-reset_es", context);

        assertThat(html)
                .contains("SAPCyTI")
                .contains("Recuperación de contraseña")
                .contains("Roberto Juárez")
                .contains("https://sapcyti.site/auth/reset-password?token=abc-123")
                .contains("30 minutos")
                .contains("Posgrado en Ciencias y Tecnologías de la Información")
                .doesNotContain("—")
                .doesNotContain("Portal de Posgrado")
                .doesNotContain("Portal Académico Institucional");
    }

    @Test
    @DisplayName("password-reset_es renders safely when userName is null")
    void testPasswordResetEsNullUserName() {
        Context context = new Context(Locale.forLanguageTag("es"));
        context.setVariable("resetUrl", "https://sapcyti.site/auth/reset-password?token=abc-123");

        String html = templateEngine.process("email/password-reset_es", context);

        assertThat(html)
                .contains("SAPCyTI")
                .contains("Recuperación de contraseña")
                .contains("Estimado(a) usuario(a):")
                .contains("https://sapcyti.site/auth/reset-password?token=abc-123")
                .doesNotContain("—")
                .doesNotContain("Hola:");
    }

    @Test
    @DisplayName("password-reset_en renders correctly with clean punctuation")
    void testPasswordResetEnRendering() {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("resetUrl", "https://sapcyti.site/auth/reset-password?token=xyz-789");

        String html = templateEngine.process("email/password-reset_en", context);

        assertThat(html)
                .contains("SAPCyTI")
                .contains("Password recovery")
                .contains("https://sapcyti.site/auth/reset-password?token=xyz-789")
                .doesNotContain("—")
                .doesNotContain("Graduate Portal")
                .doesNotContain("Institutional Academic Portal");
    }

    @Test
    @DisplayName("welcome-pilot_es renders correctly with all pilot instructions and clean naming")
    void testWelcomePilotEsRendering() {
        Context context = new Context(Locale.forLanguageTag("es"));
        context.setVariable("userName", "Valente Cárdenas");
        context.setVariable("username", "valente@correo.uam.mx");
        context.setVariable("appUrl", "https://sapcyti.site");
        context.setVariable("feedbackUrl", "https://forms.gle/testFeedback");
        context.setVariable("supportEmail", "soporte@sapcyti.site");

        String html = templateEngine.process("email/welcome-pilot_es", context);

        assertThat(html)
                .contains("¡Bienvenido(a) a la prueba de SAPCyTI!")
                .contains("Valente Cárdenas")
                .contains("valente@correo.uam.mx")
                .contains("https://sapcyti.site")
                .contains("https://forms.gle/testFeedback")
                .contains("Encuesta de Inscripción")
                .contains("encuesta de feedback del sistema")
                .contains("soporte@sapcyti.site")
                .contains("Posgrado en Ciencias y Tecnologías de la Información")
                .doesNotContain("—")
                .doesNotContain("Portal de Posgrado")
                .doesNotContain("Portal Académico Institucional");
    }

    @Test
    @DisplayName("welcome-pilot_en renders correctly with clean punctuation")
    void testWelcomePilotEnRendering() {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("userName", "Valente Cárdenas");
        context.setVariable("username", "valente@correo.uam.mx");
        context.setVariable("appUrl", "https://sapcyti.site");
        context.setVariable("feedbackUrl", "https://forms.gle/testFeedback");

        String html = templateEngine.process("email/welcome-pilot_en", context);

        assertThat(html)
                .contains("Welcome to the SAPCyTI Pilot Test!")
                .contains("valente@correo.uam.mx")
                .contains("https://sapcyti.site")
                .contains("https://forms.gle/testFeedback")
                .doesNotContain("—");
    }
}

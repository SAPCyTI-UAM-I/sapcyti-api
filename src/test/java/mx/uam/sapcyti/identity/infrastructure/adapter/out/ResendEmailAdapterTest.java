package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import mx.uam.sapcyti.configuration.application.service.MailDispatchGateService;
import mx.uam.sapcyti.configuration.domain.model.MailDispatchCommand;
import mx.uam.sapcyti.configuration.domain.model.MailDispatchOutcome;
import mx.uam.sapcyti.configuration.domain.model.MailEnvelope;
import mx.uam.sapcyti.configuration.domain.model.MailMode;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.model.UpdateMailPolicyCommand;
import mx.uam.sapcyti.configuration.domain.port.in.MailDispatchPort;
import mx.uam.sapcyti.configuration.domain.port.out.MailPolicyRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.infrastructure.config.PasswordResetProperties;
import mx.uam.sapcyti.identity.infrastructure.config.ResendProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

class ResendEmailAdapterTest {

    private static final String REAL_STUDENT = "alumno.real@uam.mx";
    private static final String STUDENT_MAILBOX = "sapcytialumno@gmail.com";
    private static final String STAFF_MAILBOX = "sapcytiprofesor@gmail.com";

    private final AtomicReference<MailPolicy> policy = new AtomicReference<>();
    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;
    private RestClient resendRestClient;
    private SpringTemplateEngine templateEngine;
    private MailDispatchGateService gate;
    private ResendEmailAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        policy.set(policy(MailMode.ENABLED));
        gate = new MailDispatchGateService(policyStore(), Duration.ofSeconds(2));
        resendRestClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        templateEngine = mock(SpringTemplateEngine.class);

        when(resendRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/emails")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.body(any(Map.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());
        when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<p>reset</p>");

        adapter = adapter(gate);
    }

    @AfterEach
    void tearDown() throws Exception {
        var shutdown = MailDispatchGateService.class.getDeclaredMethod("shutdownProviderCalls");
        shutdown.setAccessible(true);
        shutdown.invoke(gate);
    }

    @Test
    @DisplayName("sendPasswordReset posts HTML email to Resend /emails")
    @SuppressWarnings("unchecked")
    void sendPasswordResetPostsToResend() {
        adapter.sendPasswordReset("alumno@uam.mx", "raw-token", Locale.forLanguageTag("es"), RoleType.STUDENT);

        Map<String, Object> body = postedBody();
        assertThat(body.get("from")).isEqualTo("SAPCyTI <soporte@sapcyti.site>");
        assertThat(body.get("to")).isEqualTo(List.of("alumno@uam.mx"));
        assertThat(body.get("subject")).isEqualTo("SAPCyTI — Recuperación de contraseña");
        assertThat(body.get("html")).isEqualTo("<p>reset</p>");
        assertThat(body).doesNotContainKeys("cc", "bcc");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(org.mockito.ArgumentMatchers.eq("email/password-reset_es"), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("resetUrl"))
                .isEqualTo("https://spa.example.com/auth/reset-password?token=raw-token");
    }

    @Test
    @DisplayName("TEST mode JSON contains only the student mailbox")
    @SuppressWarnings("unchecked")
    void testModeStudentMailbox() {
        policy.set(policy(MailMode.TEST));

        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", Locale.ENGLISH, RoleType.STUDENT);

        Map<String, Object> body = postedBody();
        assertThat(body.get("to")).isEqualTo(List.of(STUDENT_MAILBOX));
        assertThat(body).doesNotContainKeys("cc", "bcc");
        assertThat(body.toString()).doesNotContain(REAL_STUDENT);
    }

    @Test
    @DisplayName("TEST mode JSON contains only the staff mailbox for professor and coordinator")
    @SuppressWarnings("unchecked")
    void testModeStaffMailbox() {
        policy.set(policy(MailMode.TEST));

        adapter.sendPasswordReset("profesor.real@uam.mx", "raw-token", Locale.ENGLISH, RoleType.PROFESSOR);
        adapter.sendPasswordReset("coordinador.real@uam.mx", "raw-token", Locale.ENGLISH, RoleType.COORDINATOR);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(bodySpec, times(2)).body(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(body -> {
            assertThat(body.get("to")).isEqualTo(List.of(STAFF_MAILBOX));
            assertThat(body.toString()).doesNotContain("profesor.real@uam.mx");
            assertThat(body.toString()).doesNotContain("coordinador.real@uam.mx");
        });
    }

    @Test
    @DisplayName("DISABLED mode and an unclassifiable role do not call Resend")
    void suppressedAndBlockedDoNotCallProvider() {
        policy.set(MailPolicy.initial());
        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", Locale.ENGLISH, RoleType.STUDENT);

        policy.set(policy(MailMode.ENABLED));
        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", Locale.ENGLISH, RoleType.SYSTEM_ADMIN);
        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", Locale.ENGLISH, RoleType.ASSISTANT);
        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", Locale.ENGLISH, RoleType.SPEAKER);

        verify(resendRestClient, never()).post();
    }

    @Test
    @DisplayName("the Resend body copies To, Cc and Bcc from the resolved envelope")
    @SuppressWarnings("unchecked")
    void reconstructsEnvelope() {
        MailDispatchPort port = mock(MailDispatchPort.class);
        when(port.dispatch(any(MailDispatchCommand.class), any())).thenAnswer(invocation -> {
            MailDispatchPort.MailSender sender = invocation.getArgument(1);
            sender.send(new MailEnvelope(
                    List.of(STUDENT_MAILBOX),
                    List.of("copia.prueba@gmail.com"),
                    List.of("oculta.prueba@gmail.com")));
            return MailDispatchOutcome.SENT;
        });
        ResendEmailAdapter redirected = adapter(port);

        redirected.sendPasswordReset(REAL_STUDENT, "raw-token", Locale.ENGLISH, RoleType.STUDENT);

        Map<String, Object> body = postedBody();
        assertThat(body.get("to")).isEqualTo(List.of(STUDENT_MAILBOX));
        assertThat(body.get("cc")).isEqualTo(List.of("copia.prueba@gmail.com"));
        assertThat(body.get("bcc")).isEqualTo(List.of("oculta.prueba@gmail.com"));
        assertThat(body.toString()).doesNotContain(REAL_STUDENT);
    }

    @Test
    @DisplayName("sendPasswordReset wraps Resend HTTP errors")
    void sendPasswordResetWrapsHttpErrors() {
        when(responseSpec.toBodilessEntity()).thenThrow(new RestClientResponseException(
                "Bad Request", HttpStatusCode.valueOf(400), "Bad Request", null, null, null));

        assertThatThrownBy(() -> adapter.sendPasswordReset(
                        "alumno@uam.mx", "tok", Locale.ENGLISH, RoleType.STUDENT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Resend");
    }

    private ResendEmailAdapter adapter(MailDispatchPort port) {
        return new ResendEmailAdapter(
                resendRestClient,
                templateEngine,
                new PasswordResetProperties("https://spa.example.com", 30),
                new ResendProperties("re_test_key", "SAPCyTI <soporte@sapcyti.site>", "https://api.resend.com"),
                port);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postedBody() {
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(bodySpec).body(bodyCaptor.capture());
        return bodyCaptor.getValue();
    }

    private MailPolicyRepositoryPort policyStore() {
        return new MailPolicyRepositoryPort() {
            @Override
            public MailPolicy load() {
                return policy.get();
            }

            @Override
            public MailPolicy compareAndSwap(
                    UpdateMailPolicyCommand command, Long actorUserId, Instant occurredAt) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static MailPolicy policy(MailMode mode) {
        MailPolicy stored = MailPolicy.initial();
        if (mode != MailMode.DISABLED) {
            stored.changeMode(mode, 1L, Instant.parse("2026-10-12T00:00:00Z"));
        }
        return stored;
    }
}

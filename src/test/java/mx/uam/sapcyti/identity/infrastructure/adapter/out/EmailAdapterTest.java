package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

class EmailAdapterTest {

    private static final String REAL_STUDENT = "alumno.real@uam.mx";
    private static final String REAL_PROFESSOR = "profesor.real@uam.mx";
    private static final String STUDENT_MAILBOX = "sapcytialumno@gmail.com";
    private static final String STAFF_MAILBOX = "sapcytiprofesor@gmail.com";

    private final AtomicReference<MailPolicy> policy = new AtomicReference<>();
    private MailDispatchGateService gate;
    private JavaMailSender mailSender;
    private EmailAdapter adapter;

    @BeforeEach
    void setUp() {
        policy.set(MailPolicy.initial());
        gate = new MailDispatchGateService(policyStore(), Duration.ofSeconds(2));
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> newMimeMessage());
        SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
        when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<p>reset</p>");
        adapter = new EmailAdapter(
                mailSender,
                templateEngine,
                new PasswordResetProperties("https://spa.example.com", 30),
                gate);
        ReflectionTestUtils.setField(adapter, "fromAddress", "noreply@uam.mx");
    }

    @AfterEach
    void tearDown() throws Exception {
        var shutdown = MailDispatchGateService.class.getDeclaredMethod("shutdownProviderCalls");
        shutdown.setAccessible(true);
        shutdown.invoke(gate);
    }

    @Test
    @DisplayName("TEST mode MIME contains only the student mailbox")
    void testModeStudentMailbox() throws Exception {
        policy.set(policy(MailMode.TEST));

        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", null, RoleType.STUDENT);

        MimeMessage sent = sentMessage();
        assertThat(recipients(sent, Message.RecipientType.TO)).containsExactly(STUDENT_MAILBOX);
        assertThat(recipients(sent, Message.RecipientType.CC)).isEmpty();
        assertThat(recipients(sent, Message.RecipientType.BCC)).isEmpty();
        assertThat(raw(sent)).doesNotContain(REAL_STUDENT);
    }

    @Test
    @DisplayName("TEST mode MIME contains only the staff mailbox for professor and coordinator")
    void testModeStaffMailbox() throws Exception {
        policy.set(policy(MailMode.TEST));

        adapter.sendPasswordReset(REAL_PROFESSOR, "raw-token", null, RoleType.PROFESSOR);
        assertThat(recipients(sentMessage(), Message.RecipientType.TO)).containsExactly(STAFF_MAILBOX);
        assertThat(raw(sentMessage())).doesNotContain(REAL_PROFESSOR);

        adapter.sendPasswordReset("coordinador.real@uam.mx", "raw-token", null, RoleType.COORDINATOR);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, org.mockito.Mockito.times(2)).send(captor.capture());
        MimeMessage coordinator = captor.getAllValues().get(1);
        assertThat(recipients(coordinator, Message.RecipientType.TO)).containsExactly(STAFF_MAILBOX);
        assertThat(recipients(coordinator, Message.RecipientType.CC)).isEmpty();
        assertThat(recipients(coordinator, Message.RecipientType.BCC)).isEmpty();
        assertThat(raw(coordinator)).doesNotContain("coordinador.real@uam.mx");
    }

    @Test
    @DisplayName("ENABLED mode MIME uses the account address")
    void enabledModeUsesAccountAddress() throws Exception {
        policy.set(policy(MailMode.ENABLED));

        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", null, RoleType.STUDENT);

        MimeMessage sent = sentMessage();
        assertThat(recipients(sent, Message.RecipientType.TO)).containsExactly(REAL_STUDENT);
        assertThat(recipients(sent, Message.RecipientType.CC)).isEmpty();
        assertThat(recipients(sent, Message.RecipientType.BCC)).isEmpty();
    }

    @Test
    @DisplayName("DISABLED mode and an unclassifiable role do not call JavaMailSender")
    void suppressedAndBlockedDoNotCallProvider() {
        policy.set(policy(MailMode.DISABLED));
        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", null, RoleType.STUDENT);

        policy.set(policy(MailMode.ENABLED));
        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", null, RoleType.SYSTEM_ADMIN);
        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", null, RoleType.ASSISTANT);
        adapter.sendPasswordReset(REAL_STUDENT, "raw-token", null, RoleType.SPEAKER);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("the MIME message copies To, Cc and Bcc from the resolved envelope")
    void reconstructsEnvelope() throws Exception {
        MailDispatchPort port = mock(MailDispatchPort.class);
        when(port.dispatch(any(MailDispatchCommand.class), any())).thenAnswer(invocation -> {
            MailDispatchPort.MailSender sender = invocation.getArgument(1);
            sender.send(new MailEnvelope(
                    List.of(STUDENT_MAILBOX),
                    List.of("copia.prueba@gmail.com"),
                    List.of("oculta.prueba@gmail.com")));
            return MailDispatchOutcome.SENT;
        });
        EmailAdapter redirected = new EmailAdapter(
                mailSender,
                templateEngine(),
                new PasswordResetProperties("https://spa.example.com", 30),
                port);
        ReflectionTestUtils.setField(redirected, "fromAddress", "noreply@uam.mx");

        redirected.sendPasswordReset(REAL_STUDENT, "raw-token", null, RoleType.STUDENT);

        MimeMessage sent = sentMessage();
        assertThat(recipients(sent, Message.RecipientType.TO)).containsExactly(STUDENT_MAILBOX);
        assertThat(recipients(sent, Message.RecipientType.CC)).containsExactly("copia.prueba@gmail.com");
        assertThat(recipients(sent, Message.RecipientType.BCC)).containsExactly("oculta.prueba@gmail.com");
        assertThat(raw(sent)).doesNotContain(REAL_STUDENT);
    }

    private SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
        when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<p>reset</p>");
        return templateEngine;
    }

    private MimeMessage sentMessage() throws MessagingException {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private static MimeMessage newMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private static List<String> recipients(MimeMessage message, Message.RecipientType type) throws MessagingException {
        Address[] addresses = message.getRecipients(type);
        if (addresses == null || addresses.length == 0) {
            return List.of();
        }
        return Arrays.stream(addresses).map(Address::toString).toList();
    }

    private static String raw(MimeMessage message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private MailPolicyRepositoryPort policyStore() {
        return new MailPolicyRepositoryPort() {
            @Override
            public MailPolicy load() {
                MailPolicy current = policy.get();
                if (current == null) {
                    throw new IllegalStateException("Mail policy row is missing");
                }
                return current;
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

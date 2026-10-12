package mx.uam.sapcyti.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import mx.uam.sapcyti.configuration.domain.exception.MailPolicyUnavailableException;
import mx.uam.sapcyti.configuration.domain.model.MailMode;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.port.in.GetMailPolicyInputPort;
import mx.uam.sapcyti.identity.domain.model.PasswordResetToken;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.EmailPort;
import mx.uam.sapcyti.identity.domain.port.out.SecureTokenGeneratorPort;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.identity.infrastructure.config.PasswordResetProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordUseCaseTest {

    private static final Instant CHANGED_AT = Instant.parse("2026-10-12T00:00:00Z");

    @Mock private UserRepositoryPort userRepository;
    @Mock private SecureTokenGeneratorPort tokenGenerator;
    @Mock private EmailPort emailPort;
    @Mock private GetMailPolicyInputPort mailPolicy;

    private ForgotPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties("http://localhost:4200", 30);
        useCase = new ForgotPasswordUseCase(
                userRepository, tokenGenerator, emailPort, properties, mailPolicy);
        lenient().when(mailPolicy.get()).thenReturn(policy(MailMode.ENABLED));
        lenient().when(tokenGenerator.generateToken()).thenReturn("raw-token");
    }

    @Test
    @DisplayName("registered email generates token, saves user and sends email")
    void registeredEmail() {
        User user = activeUser("alumno@uam.mx", RoleType.STUDENT);

        ForgotPasswordUseCase.ForgotPasswordResult result =
                useCase.execute("alumno@uam.mx", Locale.forLanguageTag("es"));

        assertThat(result.isResetRequested()).isTrue();
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(user.getPasswordResetToken()).isNotNull();
        assertThat(user.getPasswordResetToken().getTokenHash())
                .isEqualTo(PasswordResetToken.hashToken("raw-token"));
        verify(emailPort).sendPasswordReset(
                eq("alumno@uam.mx"), eq("raw-token"), any(Locale.class), eq(RoleType.STUDENT));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("token save finishes before the mail port is called")
    void savesTokenBeforeSending() {
        User user = activeUser("alumno@uam.mx", RoleType.STUDENT);

        useCase.execute("alumno@uam.mx", Locale.ENGLISH);

        InOrder order = inOrder(userRepository, emailPort);
        order.verify(userRepository).save(user);
        order.verify(emailPort).sendPasswordReset(
                eq("alumno@uam.mx"), eq("raw-token"), any(Locale.class), eq(RoleType.STUDENT));
    }

    @Test
    @DisplayName("TEST mode still sends for a student")
    void testModeSendsForStudent() {
        when(mailPolicy.get()).thenReturn(policy(MailMode.TEST));
        activeUser("alumno@uam.mx", RoleType.STUDENT);

        ForgotPasswordUseCase.ForgotPasswordResult result =
                useCase.execute("alumno@uam.mx", Locale.ENGLISH);

        assertThat(result.isResetRequested()).isTrue();
        verify(emailPort).sendPasswordReset(
                eq("alumno@uam.mx"), eq("raw-token"), any(Locale.class), eq(RoleType.STUDENT));
    }

    @Test
    @DisplayName("TEST mode still sends for professor and coordinator")
    void testModeSendsForStaff() {
        when(mailPolicy.get()).thenReturn(policy(MailMode.TEST));
        User professor = activeUser("profesor@uam.mx", RoleType.PROFESSOR);

        assertThat(useCase.execute("profesor@uam.mx", Locale.ENGLISH).isResetRequested()).isTrue();
        verify(emailPort).sendPasswordReset(
                eq("profesor@uam.mx"), eq("raw-token"), any(Locale.class), eq(RoleType.PROFESSOR));

        User coordinator = activeUser("coordinador@uam.mx", RoleType.COORDINATOR);
        assertThat(useCase.execute("coordinador@uam.mx", Locale.ENGLISH).isResetRequested()).isTrue();
        verify(emailPort).sendPasswordReset(
                eq("coordinador@uam.mx"), eq("raw-token"), any(Locale.class), eq(RoleType.COORDINATOR));
        verify(userRepository).save(professor);
        verify(userRepository).save(coordinator);
    }

    @Test
    @DisplayName("ENABLED mode sends the account address for a student")
    void enabledModeSendsAccountAddress() {
        activeUser("alumno@uam.mx", RoleType.STUDENT);

        useCase.execute("alumno@uam.mx", Locale.ENGLISH);

        verify(emailPort).sendPasswordReset(
                eq("alumno@uam.mx"), eq("raw-token"), any(Locale.class), eq(RoleType.STUDENT));
    }

    @Test
    @DisplayName("unclassifiable roles do not save a token or call the mail port")
    void unclassifiableRoleDoesNotSend() {
        for (RoleType role : new RoleType[] {RoleType.SYSTEM_ADMIN, RoleType.ASSISTANT, RoleType.SPEAKER}) {
            User user = activeUser(role.name().toLowerCase() + "@uam.mx", role);
            user.setPasswordResetToken(PasswordResetToken.create("previous-hash", 30));

            ForgotPasswordUseCase.ForgotPasswordResult result =
                    useCase.execute(user.getEmail(), Locale.ENGLISH);

            assertThat(result.isResetRequested()).isFalse();
            assertThat(user.getPasswordResetToken().getTokenHash()).isEqualTo("previous-hash");
        }
        verify(emailPort, never()).sendPasswordReset(any(), any(), any(), any());
        verify(userRepository, never()).save(any());
        verify(tokenGenerator, never()).generateToken();
    }

    @Test
    @DisplayName("DISABLED mode does not persist a token or call the mail port")
    void disabledModeSkipsToken() {
        when(mailPolicy.get()).thenReturn(MailPolicy.initial());
        User user = activeUser("alumno@uam.mx", RoleType.STUDENT);
        user.setPasswordResetToken(PasswordResetToken.create("previous-hash", 30));

        ForgotPasswordUseCase.ForgotPasswordResult result =
                useCase.execute("alumno@uam.mx", Locale.ENGLISH);

        assertThat(result.isResetRequested()).isFalse();
        assertThat(user.getPasswordResetToken().getTokenHash()).isEqualTo("previous-hash");
        verify(userRepository, never()).save(any());
        verify(emailPort, never()).sendPasswordReset(any(), any(), any(), any());
        verify(tokenGenerator, never()).generateToken();
    }

    @Test
    @DisplayName("an unreadable policy does not persist a token or call the mail port")
    void unreadablePolicySkipsToken() {
        doThrow(new MailPolicyUnavailableException()).when(mailPolicy).get();
        User user = activeUser("alumno@uam.mx", RoleType.STUDENT);

        ForgotPasswordUseCase.ForgotPasswordResult unavailable =
                useCase.execute("alumno@uam.mx", Locale.ENGLISH);

        assertThat(unavailable.isResetRequested()).isFalse();
        assertThat(user.getPasswordResetToken()).isNull();

        doThrow(new IllegalStateException("Mail policy row is missing")).when(mailPolicy).get();
        ForgotPasswordUseCase.ForgotPasswordResult missing =
                useCase.execute("alumno@uam.mx", Locale.ENGLISH);

        assertThat(missing.isResetRequested()).isFalse();
        verify(userRepository, never()).save(any());
        verify(emailPort, never()).sendPasswordReset(any(), any(), any(), any());
    }

    @Test
    @DisplayName("unregistered email does not generate token or send email")
    void unregisteredEmail() {
        when(userRepository.findByEmail("missing@uam.mx")).thenReturn(Optional.empty());

        ForgotPasswordUseCase.ForgotPasswordResult result =
                useCase.execute("missing@uam.mx", Locale.ENGLISH);

        assertThat(result.isResetRequested()).isFalse();
        verify(tokenGenerator, never()).generateToken();
        verify(emailPort, never()).sendPasswordReset(any(), any(), any(), any());
        verify(userRepository, never()).save(any());
        verify(mailPolicy, never()).get();
    }

    @Test
    @DisplayName("new request replaces previous token")
    void replacesPreviousToken() {
        User user = activeUser("alumno@uam.mx", RoleType.STUDENT);
        user.setPasswordResetToken(PasswordResetToken.create("old-hash", 30));
        when(tokenGenerator.generateToken()).thenReturn("new-token");

        useCase.execute("alumno@uam.mx", Locale.ENGLISH);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordResetToken().getTokenHash())
                .isEqualTo(PasswordResetToken.hashToken("new-token"));
    }

    @Test
    @DisplayName("inactive user is treated as unregistered")
    void inactiveUser() {
        User user = activeUser("inactive@uam.mx", RoleType.STUDENT);
        user.setActive(false);

        ForgotPasswordUseCase.ForgotPasswordResult result =
                useCase.execute("inactive@uam.mx", Locale.ENGLISH);

        assertThat(result.isResetRequested()).isFalse();
        verify(emailPort, never()).sendPasswordReset(any(), any(), any(), any());
        verify(mailPolicy, never()).get();
    }

    private User activeUser(String email, RoleType role) {
        User user = new User(email, "hash", role, role == RoleType.SYSTEM_ADMIN ? null : 1L);
        ReflectionTestUtils.setField(user, "id", 10L);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        return user;
    }

    private static MailPolicy policy(MailMode mode) {
        MailPolicy policy = MailPolicy.initial();
        if (mode != MailMode.DISABLED) {
            policy.changeMode(mode, 1L, CHANGED_AT);
        }
        return policy;
    }
}

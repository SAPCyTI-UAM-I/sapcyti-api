package mx.uam.sapcyti.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordUseCaseTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private SecureTokenGeneratorPort tokenGenerator;
    @Mock private EmailPort emailPort;

    private ForgotPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties("http://localhost:4200", 30);
        useCase = new ForgotPasswordUseCase(userRepository, tokenGenerator, emailPort, properties);
    }

    @Test
    @DisplayName("registered email generates token, saves user and sends email")
    void registeredEmail() {
        User user = new User("alumno@uam.mx", "hash", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(user, "id", 10L);
        when(userRepository.findByEmail("alumno@uam.mx")).thenReturn(Optional.of(user));
        when(tokenGenerator.generateToken()).thenReturn("raw-token");

        ForgotPasswordUseCase.ForgotPasswordResult result =
                useCase.execute("alumno@uam.mx", Locale.forLanguageTag("es"));

        assertThat(result.isResetRequested()).isTrue();
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(user.getPasswordResetToken()).isNotNull();
        assertThat(user.getPasswordResetToken().getTokenHash())
                .isEqualTo(PasswordResetToken.hashToken("raw-token"));
        verify(emailPort).sendPasswordReset(eq("alumno@uam.mx"), eq("raw-token"), any(Locale.class));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("unregistered email does not generate token or send email")
    void unregisteredEmail() {
        when(userRepository.findByEmail("missing@uam.mx")).thenReturn(Optional.empty());

        ForgotPasswordUseCase.ForgotPasswordResult result =
                useCase.execute("missing@uam.mx", Locale.ENGLISH);

        assertThat(result.isResetRequested()).isFalse();
        verify(tokenGenerator, never()).generateToken();
        verify(emailPort, never()).sendPasswordReset(any(), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("new request replaces previous token")
    void replacesPreviousToken() {
        User user = new User("alumno@uam.mx", "hash", RoleType.STUDENT, 1L);
        user.setPasswordResetToken(PasswordResetToken.create("old-hash", 30));
        when(userRepository.findByEmail("alumno@uam.mx")).thenReturn(Optional.of(user));
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
        User user = new User("inactive@uam.mx", "hash", RoleType.STUDENT, 1L);
        user.setActive(false);
        when(userRepository.findByEmail("inactive@uam.mx")).thenReturn(Optional.of(user));

        ForgotPasswordUseCase.ForgotPasswordResult result =
                useCase.execute("inactive@uam.mx", Locale.ENGLISH);

        assertThat(result.isResetRequested()).isFalse();
        verify(emailPort, never()).sendPasswordReset(any(), any(), any());
    }
}

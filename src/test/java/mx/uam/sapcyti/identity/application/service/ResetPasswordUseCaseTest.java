package mx.uam.sapcyti.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import mx.uam.sapcyti.identity.domain.exception.ExpiredResetTokenException;
import mx.uam.sapcyti.identity.domain.exception.InvalidResetTokenException;
import mx.uam.sapcyti.identity.domain.exception.UsedResetTokenException;
import mx.uam.sapcyti.identity.domain.model.PasswordResetToken;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.PasswordEncoderPort;
import mx.uam.sapcyti.identity.domain.port.out.RefreshTokenRepositoryPort;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseTest {

    private static final String RAW_TOKEN = "valid-raw-token";

    @Mock private UserRepositoryPort userRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private RefreshTokenRepositoryPort refreshTokenRepository;

    private ResetPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResetPasswordUseCase(userRepository, passwordEncoder, refreshTokenRepository);
    }

    @Test
    @DisplayName("valid token updates password, marks used and revokes refresh tokens")
    void validToken() {
        User user = userWithToken(PasswordResetToken.create(PasswordResetToken.hashToken(RAW_TOKEN), 30));
        ReflectionTestUtils.setField(user, "id", 5L);
        when(userRepository.findByPasswordResetTokenHash(PasswordResetToken.hashToken(RAW_TOKEN)))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewS3cur3!Pass")).thenReturn("new-hash");

        ResetPasswordUseCase.ResetPasswordResult result =
                useCase.execute(RAW_TOKEN, "NewS3cur3!Pass");

        assertThat(result.getUserId()).isEqualTo(5L);
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getPasswordResetToken().isUsed()).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(5L);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("invalid token throws InvalidResetTokenException")
    void invalidToken() {
        when(userRepository.findByPasswordResetTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("unknown", "NewS3cur3!Pass"))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    @DisplayName("expired token throws ExpiredResetTokenException")
    void expiredToken() {
        PasswordResetToken expired = new PasswordResetToken(
                PasswordResetToken.hashToken(RAW_TOKEN),
                Instant.now().minus(1, ChronoUnit.MINUTES));
        User user = userWithToken(expired);
        when(userRepository.findByPasswordResetTokenHash(PasswordResetToken.hashToken(RAW_TOKEN)))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN, "NewS3cur3!Pass"))
                .isInstanceOf(ExpiredResetTokenException.class);
    }

    @Test
    @DisplayName("used token throws UsedResetTokenException")
    void usedToken() {
        PasswordResetToken used = PasswordResetToken.create(PasswordResetToken.hashToken(RAW_TOKEN), 30);
        used.markAsUsed();
        User user = userWithToken(used);
        when(userRepository.findByPasswordResetTokenHash(PasswordResetToken.hashToken(RAW_TOKEN)))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN, "NewS3cur3!Pass"))
                .isInstanceOf(UsedResetTokenException.class);
    }

    private User userWithToken(PasswordResetToken token) {
        User user = new User("alumno@uam.mx", "old-hash", RoleType.STUDENT, 1L);
        user.setPasswordResetToken(token);
        return user;
    }
}

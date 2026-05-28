package mx.uam.sapcyti.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import mx.uam.sapcyti.identity.domain.model.RefreshToken;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.in.AuthInputPort;
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
class AuthServiceTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        AuthInputPort.LoginCommand command = AuthInputPort.LoginCommand.builder()
                .email("test@uam.mx")
                .password("password")
                .build();

        User user = new User("test@uam.mx", "hashed", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByEmail("test@uam.mx")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.hashToken(anyString())).thenReturn("hashed-refresh");

        AuthInputPort.LoginResult result = authService.login(command);

        assertThat(result.getAuthResponse().getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("should throw exception with invalid credentials")
    void shouldThrowExceptionWithInvalidCredentials() {
        AuthInputPort.LoginCommand command = AuthInputPort.LoginCommand.builder()
                .email("test@uam.mx")
                .password("wrong")
                .build();

        when(userRepository.findByEmail("test@uam.mx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("should refresh token successfully")
    void shouldRefreshSuccessfully() {
        String refreshTokenPlain = "plain-token";
        String tokenHash = "hashed-token";
        User user = new User("test@uam.mx", "hashed", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(user, "id", 1L);
        RefreshToken refreshTokenEntity = new RefreshToken(tokenHash, Instant.now().plus(1, ChronoUnit.DAYS), "device", user);

        when(jwtService.hashToken(refreshTokenPlain)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(refreshTokenEntity));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");

        mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.AuthResponse result = authService.refresh(refreshTokenPlain);

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("should logout successfully")
    void shouldLogoutSuccessfully() {
        String refreshTokenPlain = "plain-token";
        String tokenHash = "hashed-token";

        when(jwtService.hashToken(refreshTokenPlain)).thenReturn(tokenHash);

        authService.logout(refreshTokenPlain);

        verify(refreshTokenRepository).deleteByTokenHash(tokenHash);
    }

    @Test
    @DisplayName("should throw exception when refresh token is invalid")
    void shouldThrowExceptionWhenRefreshTokenIsInvalid() {
        String refreshTokenPlain = "plain-token";
        String tokenHash = "hashed-token";

        when(jwtService.hashToken(refreshTokenPlain)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(refreshTokenPlain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid refresh token");
    }
}

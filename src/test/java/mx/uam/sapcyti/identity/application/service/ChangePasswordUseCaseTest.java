package mx.uam.sapcyti.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import mx.uam.sapcyti.identity.application.model.AuthenticatedUser;
import mx.uam.sapcyti.identity.domain.exception.IncorrectCurrentPasswordException;
import mx.uam.sapcyti.identity.domain.exception.PasswordChangeForbiddenException;
import mx.uam.sapcyti.identity.domain.exception.UserNotFoundException;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.PasswordEncoderPort;
import mx.uam.sapcyti.identity.domain.port.out.RefreshTokenRepositoryPort;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

    private static final String CURRENT_PASSWORD = "OldP@ssword1";
    private static final String NEW_PASSWORD = "NewS3cur3!Pass";

    @Mock private UserRepositoryPort userRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private RefreshTokenRepositoryPort refreshTokenRepository;

    private ChangePasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangePasswordUseCase(userRepository, passwordEncoder, refreshTokenRepository);
    }

    @Test
    @DisplayName("self-change updates password and revokes refresh tokens")
    void selfChangeSuccess() {
        User target = student(50L, 10L);
        when(userRepository.findById(50L)).thenReturn(Optional.of(target));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "old-hash")).thenReturn(true);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("new-hash");

        ChangePasswordUseCase.ChangePasswordResult result = useCase.execute(
                50L,
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                caller(50L, RoleType.STUDENT, 10L));

        assertThat(result.getUserId()).isEqualTo(50L);
        assertThat(result.getChangeType()).isEqualTo("SELF");
        assertThat(target.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenRepository).revokeAllByUserId(50L);
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("coordinator changes another user in same tenant")
    void coordinatorChangeSuccess() {
        User target = student(50L, 10L);
        when(userRepository.findById(50L)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("new-hash");

        ChangePasswordUseCase.ChangePasswordResult result = useCase.execute(
                50L,
                null,
                NEW_PASSWORD,
                caller(1L, RoleType.COORDINATOR, 10L));

        assertThat(result.getChangeType()).isEqualTo("COORDINATOR");
        assertThat(target.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenRepository).revokeAllByUserId(50L);
    }

    @Test
    @DisplayName("wrong current password throws IncorrectCurrentPasswordException")
    void wrongCurrentPassword() {
        User target = student(50L, 10L);
        when(userRepository.findById(50L)).thenReturn(Optional.of(target));
        when(passwordEncoder.matches("WrongOldPass", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(
                        50L,
                        "WrongOldPass",
                        NEW_PASSWORD,
                        caller(50L, RoleType.STUDENT, 10L)))
                .isInstanceOf(IncorrectCurrentPasswordException.class);
    }

    @Test
    @DisplayName("self-change without current password is rejected")
    void missingCurrentPassword() {
        User target = student(50L, 10L);
        when(userRepository.findById(50L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> useCase.execute(
                        50L,
                        "",
                        NEW_PASSWORD,
                        caller(50L, RoleType.STUDENT, 10L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current password is required");
    }

    @Test
    @DisplayName("non-coordinator cannot change another user's password")
    void forbiddenForOtherUser() {
        User target = student(99L, 10L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> useCase.execute(
                        99L,
                        CURRENT_PASSWORD,
                        NEW_PASSWORD,
                        caller(50L, RoleType.STUDENT, 10L)))
                .isInstanceOf(PasswordChangeForbiddenException.class);
    }

    @Test
    @DisplayName("coordinator cannot change password for user in another program")
    void coordinatorCrossTenant() {
        User target = student(50L, 99L);
        when(userRepository.findById(50L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> useCase.execute(
                        50L,
                        null,
                        NEW_PASSWORD,
                        caller(1L, RoleType.COORDINATOR, 10L)))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    @DisplayName("system admin can change any user's password")
    void systemAdminChange() {
        User target = student(50L, 99L);
        when(userRepository.findById(50L)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("new-hash");

        ChangePasswordUseCase.ChangePasswordResult result = useCase.execute(
                50L,
                null,
                NEW_PASSWORD,
                caller(2L, RoleType.SYSTEM_ADMIN, null));

        assertThat(result.getChangeType()).isEqualTo("COORDINATOR");
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("unknown user throws UserNotFoundException")
    void userNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
                        404L,
                        CURRENT_PASSWORD,
                        NEW_PASSWORD,
                        caller(404L, RoleType.STUDENT, 10L)))
                .isInstanceOf(UserNotFoundException.class);
    }

    private static User student(Long id, Long programId) {
        User user = new User("alumno@uam.mx", "old-hash", RoleType.STUDENT, programId);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static AuthenticatedUser caller(Long userId, RoleType role, Long programId) {
        return AuthenticatedUser.builder()
                .userId(userId)
                .role(role)
                .graduateProgramId(programId)
                .build();
    }
}

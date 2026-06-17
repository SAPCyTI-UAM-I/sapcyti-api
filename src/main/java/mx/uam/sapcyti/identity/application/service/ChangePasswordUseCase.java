package mx.uam.sapcyti.identity.application.service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final RefreshTokenRepositoryPort refreshTokenRepository;

    @Transactional
    public ChangePasswordResult execute(
            Long targetUserId,
            String currentPassword,
            String newPassword,
            AuthenticatedUser caller) {

        User target = userRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);

        boolean isSelf = caller.getUserId().equals(targetUserId);
        boolean isCoordinator = caller.getRole() == RoleType.COORDINATOR;
        boolean isSystemAdmin = caller.getRole() == RoleType.SYSTEM_ADMIN;

        if (isSelf) {
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new IllegalArgumentException("Current password is required");
            }
            if (!passwordEncoder.matches(currentPassword, target.getPasswordHash())) {
                throw new IncorrectCurrentPasswordException();
            }
        } else if (isCoordinator || isSystemAdmin) {
            if (isCoordinator) {
                assertSameTenant(caller, target);
            }
        } else {
            throw new PasswordChangeForbiddenException();
        }

        target.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenRepository.revokeAllByUserId(target.getId());
        userRepository.save(target);

        return ChangePasswordResult.builder()
                .userId(target.getId())
                .role(target.getRole().name())
                .graduateProgramId(target.getGraduateProgramId())
                .changeType(isSelf ? "SELF" : "COORDINATOR")
                .build();
    }

    private void assertSameTenant(AuthenticatedUser caller, User target) {
        Long callerProgramId = caller.getGraduateProgramId();
        Long targetProgramId = target.getGraduateProgramId();
        if (callerProgramId == null
                || targetProgramId == null
                || !callerProgramId.equals(targetProgramId)) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISMATCH_MESSAGE);
        }
    }

    @Value
    @Builder
    public static class ChangePasswordResult {
        Long userId;
        String role;
        Long graduateProgramId;
        String changeType;
    }
}

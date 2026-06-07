package mx.uam.sapcyti.identity.application.service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.identity.domain.exception.ExpiredResetTokenException;
import mx.uam.sapcyti.identity.domain.exception.InvalidResetTokenException;
import mx.uam.sapcyti.identity.domain.exception.UsedResetTokenException;
import mx.uam.sapcyti.identity.domain.model.PasswordResetToken;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.PasswordEncoderPort;
import mx.uam.sapcyti.identity.domain.port.out.RefreshTokenRepositoryPort;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final RefreshTokenRepositoryPort refreshTokenRepository;

    @Transactional
    public ResetPasswordResult execute(String rawToken, String newPassword) {
        String tokenHash = PasswordResetToken.hashToken(rawToken);
        User user = userRepository.findByPasswordResetTokenHash(tokenHash)
                .orElseThrow(InvalidResetTokenException::new);

        PasswordResetToken resetToken = user.getPasswordResetToken();
        if (resetToken == null) {
            throw new InvalidResetTokenException();
        }
        if (resetToken.isUsed()) {
            throw new UsedResetTokenException();
        }
        if (resetToken.isExpired()) {
            throw new ExpiredResetTokenException();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.markAsUsed();
        refreshTokenRepository.revokeAllByUserId(user.getId());
        userRepository.save(user);

        return ResetPasswordResult.builder()
                .userId(user.getId())
                .role(user.getRole().name())
                .graduateProgramId(user.getGraduateProgramId())
                .build();
    }

    @Value
    @Builder
    public static class ResetPasswordResult {
        Long userId;
        String role;
        Long graduateProgramId;
    }
}

package mx.uam.sapcyti.identity.application.service;

import java.util.Locale;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.identity.domain.model.PasswordResetToken;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.EmailPort;
import mx.uam.sapcyti.identity.domain.port.out.SecureTokenGeneratorPort;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.identity.infrastructure.config.PasswordResetProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final SecureTokenGeneratorPort tokenGenerator;
    private final EmailPort emailPort;
    private final PasswordResetProperties passwordResetProperties;

    @Transactional
    public ForgotPasswordResult execute(String email, Locale locale) {
        return userRepository.findByEmail(email)
                .filter(User::isActive)
                .map(user -> processRegisteredUser(user, locale))
                .orElse(ForgotPasswordResult.notRequested());
    }

    private ForgotPasswordResult processRegisteredUser(User user, Locale locale) {
        String rawToken = tokenGenerator.generateToken();
        String tokenHash = PasswordResetToken.hashToken(rawToken);
        PasswordResetToken resetToken = PasswordResetToken.create(tokenHash, passwordResetProperties.ttlMinutes());
        user.setPasswordResetToken(resetToken);
        userRepository.save(user);
        emailPort.sendPasswordReset(user.getEmail(), rawToken, locale);
        return ForgotPasswordResult.requested(user.getId(), user.getRole().name(), user.getGraduateProgramId());
    }

    @Value
    @Builder
    public static class ForgotPasswordResult {
        boolean resetRequested;
        Long userId;
        String role;
        Long graduateProgramId;

        static ForgotPasswordResult requested(Long userId, String role, Long graduateProgramId) {
            return ForgotPasswordResult.builder()
                    .resetRequested(true)
                    .userId(userId)
                    .role(role)
                    .graduateProgramId(graduateProgramId)
                    .build();
        }

        static ForgotPasswordResult notRequested() {
            return ForgotPasswordResult.builder()
                    .resetRequested(false)
                    .build();
        }
    }
}

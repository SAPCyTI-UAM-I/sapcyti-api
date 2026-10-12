package mx.uam.sapcyti.identity.application.service;

import java.util.Locale;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ForgotPasswordUseCase.class);

    private final UserRepositoryPort userRepository;
    private final SecureTokenGeneratorPort tokenGenerator;
    private final EmailPort emailPort;
    private final PasswordResetProperties passwordResetProperties;
    private final GetMailPolicyInputPort mailPolicy;

    /**
     * Consults the current mail policy before any token write. The token is stored
     * only when this attempt will send, and that save commits before
     * {@link EmailPort#sendPasswordReset}. The adapter reads the mode again at the
     * provider call; this method does not pass a frozen mode.
     */
    public ForgotPasswordResult execute(String email, Locale locale) {
        return userRepository.findByEmail(email)
                .filter(User::isActive)
                .map(user -> processRegisteredUser(user, locale))
                .orElse(ForgotPasswordResult.notRequested());
    }

    private ForgotPasswordResult processRegisteredUser(User user, Locale locale) {
        if (!willSend(readMode(), user.getRole())) {
            return ForgotPasswordResult.notRequested();
        }
        String rawToken = tokenGenerator.generateToken();
        String tokenHash = PasswordResetToken.hashToken(rawToken);
        PasswordResetToken resetToken = PasswordResetToken.create(tokenHash, passwordResetProperties.ttlMinutes());
        user.setPasswordResetToken(resetToken);
        userRepository.save(user);
        emailPort.sendPasswordReset(user.getEmail(), rawToken, locale, user.getRole());
        return ForgotPasswordResult.requested(user.getId(), user.getRole().name(), user.getGraduateProgramId());
    }

    /**
     * @return the mode just read, or null when the policy cannot be read
     */
    private MailMode readMode() {
        try {
            MailPolicy policy = mailPolicy.get();
            if (policy == null) {
                return null;
            }
            return policy.getMode();
        } catch (MailPolicyUnavailableException ex) {
            LOG.warn("Mail policy could not be read before creating a password reset token");
            return null;
        } catch (RuntimeException ex) {
            LOG.warn("Mail policy could not be read before creating a password reset token", ex);
            return null;
        }
    }

    /**
     * Token and provider call happen only for a classifiable role while the mode
     * just read is {@link MailMode#TEST} or {@link MailMode#ENABLED}.
     */
    private static boolean willSend(MailMode mode, RoleType role) {
        if (mode != MailMode.TEST && mode != MailMode.ENABLED) {
            return false;
        }
        return role == RoleType.STUDENT
                || role == RoleType.PROFESSOR
                || role == RoleType.COORDINATOR;
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

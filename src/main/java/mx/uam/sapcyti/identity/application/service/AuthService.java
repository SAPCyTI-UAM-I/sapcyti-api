package mx.uam.sapcyti.identity.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.domain.exception.InvalidCredentialsException;
import mx.uam.sapcyti.identity.domain.exception.InvalidRefreshTokenException;
import mx.uam.sapcyti.identity.domain.model.RefreshToken;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.in.AuthInputPort;
import mx.uam.sapcyti.identity.domain.port.out.PasswordEncoderPort;
import mx.uam.sapcyti.identity.domain.port.out.RefreshTokenRepositoryPort;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.AuthResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthInputPort {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByEmail(command.getEmail())
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(command.getPassword(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenPlain = UUID.randomUUID().toString();
        
        long refreshTtlDays = command.isRememberMe() ? 30 : 7;
        Instant expiresAt = Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS);
        
        String tokenHash = jwtService.hashToken(refreshTokenPlain);
        
        RefreshToken refreshTokenEntity = new RefreshToken(tokenHash, expiresAt, command.getDeviceInfo(), user);
        user.getRefreshTokens().add(refreshTokenEntity);
        userRepository.save(user);

        return LoginResult.builder()
                .authResponse(AuthResponse.builder()
                        .accessToken(accessToken)
                        .expiresIn(900)
                        .role(user.getRole())
                        .build())
                .refreshToken(refreshTokenPlain)
                .refreshExpiresIn(refreshTtlDays * 24 * 3600)
                .userId(user.getId())
                .graduateProgramId(user.getGraduateProgramId())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshTokenPlain) {
        String tokenHash = jwtService.hashToken(refreshTokenPlain);
        RefreshToken entity = refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(t -> !t.isRevoked())
                .filter(t -> !t.isExpired())
                .orElseThrow(InvalidRefreshTokenException::new);

        User user = entity.getUser();
        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(900)
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenPlain) {
        String tokenHash = jwtService.hashToken(refreshTokenPlain);
        refreshTokenRepository.deleteByTokenHash(tokenHash);
    }
}

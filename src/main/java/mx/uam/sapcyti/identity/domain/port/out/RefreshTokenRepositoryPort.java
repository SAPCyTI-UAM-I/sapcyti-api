package mx.uam.sapcyti.identity.domain.port.out;

import java.util.Optional;
import mx.uam.sapcyti.identity.domain.model.RefreshToken;

public interface RefreshTokenRepositoryPort {
    Optional<RefreshToken> findByTokenHash(String hash);
    void deleteByTokenHash(String hash);
    void revokeAllByUserId(Long userId);
}

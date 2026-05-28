package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.domain.model.RefreshToken;
import mx.uam.sapcyti.identity.domain.port.out.RefreshTokenRepositoryPort;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataRefreshTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RefreshTokenJpaAdapter implements RefreshTokenRepositoryPort {

    private final SpringDataRefreshTokenRepository repository;

    @Override
    public Optional<RefreshToken> findByTokenHash(String hash) {
        return repository.findByTokenHash(hash);
    }

    @Override
    @Transactional
    public void deleteByTokenHash(String hash) {
        repository.deleteByTokenHash(hash);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId) {
        repository.revokeAllByUserId(userId);
    }
}

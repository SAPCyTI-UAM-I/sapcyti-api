package mx.uam.sapcyti.identity.domain.port.out;

import java.util.Optional;
import mx.uam.sapcyti.identity.domain.model.User;

public interface UserRepositoryPort {
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    Optional<User> findByPasswordResetTokenHash(String tokenHash);
    User save(User user);
    boolean existsByEmail(String email);
}

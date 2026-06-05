package mx.uam.sapcyti.identity.infrastructure.adapter.out.repository;

import java.util.Optional;
import mx.uam.sapcyti.identity.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPasswordResetTokenTokenHash(String tokenHash);
    boolean existsByEmail(String email);
}

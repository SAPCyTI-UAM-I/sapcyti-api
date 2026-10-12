package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data repository for {@link MailPolicy}. Internal to the adapter.
 */
interface SpringDataMailPolicyRepository extends JpaRepository<MailPolicy, Integer> {

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT policy FROM MailPolicy policy
            WHERE policy.id = :id
            """)
    Optional<MailPolicy> findByIdForUpdate(@Param("id") Integer id);
}

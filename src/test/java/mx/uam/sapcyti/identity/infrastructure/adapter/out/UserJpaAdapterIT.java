package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(UserJpaAdapter.class)
class UserJpaAdapterIT {

    @Autowired
    private UserJpaAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createQuery("DELETE FROM RefreshToken").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM User").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("should save a User and retrieve it by email")
    void shouldSaveAndFindByEmail() {
        User user = new User("test@uam.mx", "hash", RoleType.STUDENT, 1L);

        adapter.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = adapter.findByEmail("test@uam.mx");
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(RoleType.STUDENT);
        assertThat(found.get().getGraduateProgramId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should fail on duplicate email")
    void shouldFailOnDuplicateEmail() {
        User user1 = new User("test@uam.mx", "hash1", RoleType.STUDENT, 1L);
        entityManager.persist(user1);
        entityManager.flush();

        User user2 = new User("test@uam.mx", "hash2", RoleType.COORDINATOR, 1L);
        assertThatThrownBy(() -> {
            adapter.save(user2);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        User user = new User("test@uam.mx", "hash", RoleType.STUDENT, 1L);
        entityManager.persist(user);
        entityManager.flush();

        assertThat(adapter.existsByEmail("test@uam.mx")).isTrue();
        assertThat(adapter.existsByEmail("nonexistent@uam.mx")).isFalse();
    }
}

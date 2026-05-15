package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;

/**
 * Adapter test for {@link GraduateProgramJpaAdapter}.
 *
 * <p>Uses {@code @DataJpaTest} with H2 in PostgreSQL compatibility mode.
 * Flyway is disabled; Hibernate creates tables from JPA annotations
 * (via {@code spring.jpa.hibernate.ddl-auto=create-drop} in test profile).
 *
 * @see GraduateProgramJpaAdapter
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(GraduateProgramJpaAdapter.class)
class GraduateProgramJpaAdapterTest {

    @Autowired
    private GraduateProgramJpaAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Clean slate for each test
        entityManager.getEntityManager()
            .createQuery("DELETE FROM GraduateProgram").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("should save a GraduateProgram and retrieve it by id")
    void shouldSaveAndFindById() {
        // given
        GraduateProgram program = new GraduateProgram(
            "Ciencias y Tecnologías de la Información", "CBI");

        // when
        GraduateProgram saved = adapter.save(program);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(saved.getId()).isNotNull();
        Optional<GraduateProgram> found = adapter.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName())
            .isEqualTo("Ciencias y Tecnologías de la Información");
        assertThat(found.get().getDivision()).isEqualTo("CBI");
    }

    @Test
    @DisplayName("should return all graduate programs")
    void shouldFindAll() {
        // given
        entityManager.persist(
            new GraduateProgram("PCyTI", "CBI"));
        entityManager.persist(
            new GraduateProgram("Energía y Medio Ambiente", "CBI"));
        entityManager.persist(
            new GraduateProgram("Ciencias Sociales", "CSH"));
        entityManager.flush();
        entityManager.clear();

        // when
        List<GraduateProgram> all = adapter.findAll();

        // then
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("should return true when a program with the name exists")
    void shouldReturnTrueWhenNameExists() {
        // given
        entityManager.persist(new GraduateProgram("PCyTI", "CBI"));
        entityManager.flush();
        entityManager.clear();

        // when / then
        assertThat(adapter.existsByName("PCyTI")).isTrue();
        assertThat(adapter.existsByName("Nonexistent")).isFalse();
    }

    @Test
    @DisplayName("should fail on duplicate program name — UNIQUE constraint")
    void shouldFailOnDuplicateName() {
        // given
        entityManager.persist(new GraduateProgram("PCyTI", "CBI"));
        entityManager.flush();

        // when / then
        GraduateProgram duplicate = new GraduateProgram("PCyTI", "CSH");
        assertThatThrownBy(() -> {
            adapter.save(duplicate);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("should delete a program by id")
    void shouldDeleteById() {
        // given
        GraduateProgram program = entityManager.persist(
            new GraduateProgram("PCyTI", "CBI"));
        entityManager.flush();
        Long id = program.getId();
        entityManager.clear();

        // when
        adapter.deleteById(id);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(adapter.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("should return empty for non-existent id")
    void shouldReturnEmptyForNonExistentId() {
        // when / then
        assertThat(adapter.findById(999L)).isEmpty();
    }
}

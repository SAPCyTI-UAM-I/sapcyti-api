package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Adapter test for {@link ConfigurationParameterJpaAdapter}.
 *
 * <p>Uses {@code @DataJpaTest} with H2 in PostgreSQL compatibility mode.
 * Tests CRUD operations and — critically — multi-tenant isolation (QA-4):
 * parameters from one program must never be visible to another.
 *
 * @see ConfigurationParameterJpaAdapter
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(ConfigurationParameterJpaAdapter.class)
class ConfigurationParameterJpaAdapterTest {

    @Autowired
    private ConfigurationParameterJpaAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    private GraduateProgram pcyti;
    private GraduateProgram energia;

    @BeforeEach
    void setUp() {
        // Create two programs for isolation tests
        pcyti = entityManager.persist(
                new GraduateProgram(
                        "Ciencias y Tecnologías de la Información", "CBI"));
        energia = entityManager.persist(
                new GraduateProgram("Energía y Medio Ambiente", "CBI"));
        entityManager.flush();
        entityManager.clear();
        // Re-fetch managed references
        pcyti = entityManager.find(GraduateProgram.class, pcyti.getId());
        energia = entityManager.find(
                GraduateProgram.class, energia.getId());
    }

    @Test
    @DisplayName("should save and find a parameter by program id and key")
    void shouldSaveAndFindByProgramIdAndKey() {
        // given
        ConfigurationParameter param = new ConfigurationParameter(
                pcyti, "MAX_COURSES_PER_TERM", "3",
                "Maximum UEAs per term per student");
        adapter.save(param);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<ConfigurationParameter> found =
                adapter.findByGraduateProgramIdAndKey(
                        pcyti.getId(), "MAX_COURSES_PER_TERM");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getKey()).isEqualTo("MAX_COURSES_PER_TERM");
        assertThat(found.get().getValue()).isEqualTo("3");
        assertThat(found.get().getDescription())
                .isEqualTo("Maximum UEAs per term per student");
    }

    @Test
    @DisplayName("should find all parameters for a program")
    void shouldFindAllByProgramId() {
        // given
        entityManager.persist(new ConfigurationParameter(
                pcyti, "MAX_COURSES_PER_TERM", "3", null));
        entityManager.persist(new ConfigurationParameter(
                pcyti, "CREDIT_LIMIT_PER_TERM", "27", null));
        entityManager.persist(new ConfigurationParameter(
                energia, "MAX_COURSES_PER_TERM", "5", null));
        entityManager.flush();
        entityManager.clear();

        // when
        List<ConfigurationParameter> params =
                adapter.findAllByGraduateProgramId(pcyti.getId());

        // then — only pcyti's parameters
        assertThat(params).hasSize(2);
        assertThat(params)
                .extracting(ConfigurationParameter::getKey)
                .containsExactlyInAnyOrder(
                        "MAX_COURSES_PER_TERM", "CREDIT_LIMIT_PER_TERM");
    }

    @Test
    @DisplayName(
            "should fail on duplicate key per program — UNIQUE constraint")
    void shouldFailOnDuplicateKeyPerProgram() {
        // given
        entityManager.persist(new ConfigurationParameter(
                pcyti, "MAX_COURSES_PER_TERM", "3", null));
        entityManager.flush();

        // when / then
        ConfigurationParameter duplicate = new ConfigurationParameter(
                pcyti, "MAX_COURSES_PER_TERM", "5", null);
        assertThatThrownBy(() -> {
            adapter.save(duplicate);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("should allow same key in different programs")
    void shouldAllowSameKeyInDifferentPrograms() {
        // given
        entityManager.persist(new ConfigurationParameter(
                pcyti, "MAX_COURSES_PER_TERM", "3", null));
        entityManager.flush();

        // when — same key, different program
        ConfigurationParameter energiaParam = new ConfigurationParameter(
                energia, "MAX_COURSES_PER_TERM", "5", null);
        adapter.save(energiaParam);
        entityManager.flush();

        // then — no exception
        assertThat(energiaParam.getId()).isNotNull();
    }

    @Test
    @DisplayName("should delete a parameter by program id and key")
    void shouldDeleteByProgramIdAndKey() {
        // given
        entityManager.persist(new ConfigurationParameter(
                pcyti, "MAX_COURSES_PER_TERM", "3", null));
        entityManager.persist(new ConfigurationParameter(
                pcyti, "CREDIT_LIMIT_PER_TERM", "27", null));
        entityManager.flush();
        entityManager.clear();

        // when
        adapter.deleteByGraduateProgramIdAndKey(
                pcyti.getId(), "MAX_COURSES_PER_TERM");
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(adapter.findByGraduateProgramIdAndKey(
                pcyti.getId(), "MAX_COURSES_PER_TERM")).isEmpty();
        assertThat(adapter.findByGraduateProgramIdAndKey(
                pcyti.getId(), "CREDIT_LIMIT_PER_TERM")).isPresent();
    }

    @Test
    @DisplayName("should check existence by program id and key")
    void shouldCheckExistenceByProgramIdAndKey() {
        // given
        entityManager.persist(new ConfigurationParameter(
                pcyti, "MAX_COURSES_PER_TERM", "3", null));
        entityManager.flush();
        entityManager.clear();

        // when / then
        assertThat(adapter.existsByGraduateProgramIdAndKey(
                pcyti.getId(), "MAX_COURSES_PER_TERM")).isTrue();
        assertThat(adapter.existsByGraduateProgramIdAndKey(
                pcyti.getId(), "NONEXISTENT")).isFalse();
        assertThat(adapter.existsByGraduateProgramIdAndKey(
                energia.getId(), "MAX_COURSES_PER_TERM")).isFalse();
    }

    @Test
    @DisplayName("should isolate parameters between programs — QA-4 "
            + "[Gherkin: Parameter isolation between programs]")
    void shouldIsolateParametersBetweenPrograms() {
        // given — same key, different values per program
        entityManager.persist(new ConfigurationParameter(
                pcyti, "MAX_COURSES_PER_TERM", "3",
                "PCyTI limit"));
        entityManager.persist(new ConfigurationParameter(
                energia, "MAX_COURSES_PER_TERM", "5",
                "Energía limit"));
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<ConfigurationParameter> pcytiParam =
                adapter.findByGraduateProgramIdAndKey(
                        pcyti.getId(), "MAX_COURSES_PER_TERM");
        Optional<ConfigurationParameter> energiaParam =
                adapter.findByGraduateProgramIdAndKey(
                        energia.getId(), "MAX_COURSES_PER_TERM");

        // then — each program gets its own value
        assertThat(pcytiParam).isPresent();
        assertThat(pcytiParam.get().getValue()).isEqualTo("3");

        assertThat(energiaParam).isPresent();
        assertThat(energiaParam.get().getValue()).isEqualTo("5");

        // and — listing by program returns only that program's parameters
        List<ConfigurationParameter> pcytiParams =
                adapter.findAllByGraduateProgramId(pcyti.getId());
        List<ConfigurationParameter> energiaParams =
                adapter.findAllByGraduateProgramId(energia.getId());

        assertThat(pcytiParams).hasSize(1);
        assertThat(energiaParams).hasSize(1);
        assertThat(pcytiParams.get(0).getValue())
                .isNotEqualTo(energiaParams.get(0).getValue());
    }
}

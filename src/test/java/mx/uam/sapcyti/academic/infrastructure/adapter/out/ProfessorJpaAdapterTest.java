package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.defaultProfessorInformation;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.minimalProfessorPersonalData;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.professorPersonalData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.uam.sapcyti.academic.domain.model.Professor;
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
@Import(ProfessorJpaAdapter.class)
class ProfessorJpaAdapterTest {

    @Autowired
    private ProfessorJpaAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void clean() {
        entityManager.getEntityManager().createQuery("DELETE FROM Professor").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("persists professor and finds by graduate program")
    void saveAndFindByProgram() {
        Professor professor = new Professor(
                "30568", 10L, 1L, professorPersonalData(), defaultProfessorInformation());

        adapter.save(professor);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findByGraduateProgramId(1L)).hasSize(1);
        assertThat(adapter.existsByEmployeeNumber("30568")).isTrue();
        assertThat(adapter.existsByIdAndGraduateProgramId(professor.getId(), 1L)).isTrue();
        assertThat(adapter.existsByIdAndGraduateProgramId(professor.getId(), 99L)).isFalse();
    }

    @Test
    @DisplayName("rejects duplicate employee number")
    void duplicateEmployeeNumber() {
        adapter.save(new Professor(
                "30568", 10L, 1L, minimalProfessorPersonalData("A", "B"), defaultProfessorInformation()));
        entityManager.flush();

        assertThatThrownBy(() -> {
            adapter.save(new Professor(
                    "30568", 11L, 1L, minimalProfessorPersonalData("C", "D"), defaultProfessorInformation()));
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }
}

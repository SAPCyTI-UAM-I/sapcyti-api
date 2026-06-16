package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.studentPersonalData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.uam.sapcyti.academic.domain.model.Student;
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
@Import(StudentJpaAdapter.class)
class StudentJpaAdapterTest {

    @Autowired
    private StudentJpaAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void clean() {
        entityManager.getEntityManager().createQuery("DELETE FROM Student").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("persists student and finds by graduate program")
    void saveAndFindByProgram() {
        Student student = sampleStudent("2123803361", 10L);

        adapter.save(student);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findByGraduateProgramId(1L)).hasSize(1);
        assertThat(adapter.existsByEnrollmentId("2123803361")).isTrue();
    }

    @Test
    @DisplayName("rejects duplicate enrollment id")
    void duplicateEnrollmentId() {
        adapter.save(sampleStudent("2123803361", 10L));
        entityManager.flush();

        assertThatThrownBy(() -> {
            adapter.save(sampleStudent("2123803361", 11L));
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    private static Student sampleStudent(String enrollmentId, Long userId) {
        return new Student(
                enrollmentId, userId, 1L, null, studentPersonalData(), sampleAcademicInformation());
    }
}

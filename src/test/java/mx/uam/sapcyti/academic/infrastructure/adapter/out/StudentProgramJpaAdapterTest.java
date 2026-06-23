package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.studentPersonalData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
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
@Import(StudentProgramJpaAdapter.class)
class StudentProgramJpaAdapterTest {

    @Autowired
    private StudentProgramJpaAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    private Long studentId;

    @BeforeEach
    void seedStudent() {
        entityManager.getEntityManager().createQuery("DELETE FROM StudentProgramAdvisor").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM StudentProgram").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM Student").executeUpdate();
        entityManager.flush();
        entityManager.clear();

        Student student = new Student(
                "2123803361", 10L, 1L, null, studentPersonalData(), sampleAcademicInformation());
        entityManager.persist(student);
        entityManager.flush();
        studentId = student.getId();
    }

    @Test
    @DisplayName("persists program and finds by student")
    void saveAndFindByStudent() {
        StudentProgram program = new StudentProgram(
                studentId, 1L, "2123803361", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), ProgramStatus.ACTIVO);

        adapter.save(program);
        entityManager.flush();
        entityManager.clear();

        List<StudentProgram> found = adapter.findByStudentIdAndGraduateProgramId(studentId, 1L);
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getEnrollmentId()).isEqualTo("2123803361");
    }

    @Test
    @DisplayName("rejects duplicate program type for same student")
    void duplicateProgramType() {
        adapter.save(new StudentProgram(
                studentId, 1L, "2123803361", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), ProgramStatus.ACTIVO));
        entityManager.flush();

        assertThatThrownBy(() -> {
            adapter.save(new StudentProgram(
                    studentId, 1L, "2123803362", ProgramType.MAESTRIA, LocalDate.of(2024, 9, 1), ProgramStatus.ACTIVO));
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }
}

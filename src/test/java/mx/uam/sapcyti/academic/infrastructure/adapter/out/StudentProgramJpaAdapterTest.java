package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.studentPersonalData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.AcademicInformation;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
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
import org.springframework.dao.DataIntegrityViolationException;
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
                studentId, 1L, "2123803361", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), ProgramStatus.EN_INVESTIGACION);

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
                studentId, 1L, "2123803361", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), ProgramStatus.EN_INVESTIGACION));
        entityManager.flush();

        assertThatThrownBy(() -> {
            adapter.save(new StudentProgram(
                    studentId, 1L, "2123803362", ProgramType.MAESTRIA, LocalDate.of(2024, 9, 1), ProgramStatus.EN_INVESTIGACION));
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("SPEC-044: rereads specialization without changing master's and doctoral rows")
    void rereadsSpecializationWithoutChangingStoredRows() {
        Long maestriaStudentId = persistStudent("2123803350", 50L, ProgramType.MAESTRIA);
        Long doctoradoStudentId = persistStudent("2123803351", 51L, ProgramType.DOCTORADO);
        Long especializacionStudentId = persistStudent("2123803352", 52L, ProgramType.ESPECIALIZACION);

        StudentProgram maestria = adapter.save(new StudentProgram(
                maestriaStudentId, 1L, "2123803350", ProgramType.MAESTRIA,
                LocalDate.of(2020, 1, 15), ProgramStatus.EGRESADO));
        StudentProgram doctorado = adapter.save(new StudentProgram(
                doctoradoStudentId, 1L, "2123803351", ProgramType.DOCTORADO,
                LocalDate.of(2022, 1, 15), ProgramStatus.EN_INVESTIGACION));
        StudentProgram especializacion = adapter.save(new StudentProgram(
                especializacionStudentId, 1L, "2123803352", ProgramType.ESPECIALIZACION,
                LocalDate.of(2024, 1, 15), ProgramStatus.EN_INVESTIGACION));
        entityManager.flush();
        Long maestriaId = maestria.getId();
        Long doctoradoId = doctorado.getId();
        Long especializacionId = especializacion.getId();
        entityManager.clear();

        StudentProgram readMaestria = entityManager.find(StudentProgram.class, maestriaId);
        StudentProgram readDoctorado = entityManager.find(StudentProgram.class, doctoradoId);
        StudentProgram readEspecializacion = entityManager.find(StudentProgram.class, especializacionId);
        entityManager.flush();
        entityManager.clear();

        StudentProgram afterMaestria = entityManager.find(StudentProgram.class, maestriaId);
        StudentProgram afterDoctorado = entityManager.find(StudentProgram.class, doctoradoId);
        StudentProgram afterEspecializacion = entityManager.find(StudentProgram.class, especializacionId);

        assertThat(readMaestria.getProgramType()).isEqualTo(ProgramType.MAESTRIA);
        assertThat(readMaestria.getStatus()).isEqualTo(ProgramStatus.EGRESADO);
        assertThat(readDoctorado.getProgramType()).isEqualTo(ProgramType.DOCTORADO);
        assertThat(readDoctorado.getStatus()).isEqualTo(ProgramStatus.EN_INVESTIGACION);
        assertThat(readEspecializacion.getProgramType()).isEqualTo(ProgramType.ESPECIALIZACION);
        assertThat(readEspecializacion.getStatus()).isEqualTo(ProgramStatus.EN_INVESTIGACION);
        assertThat(afterMaestria.getProgramType()).isEqualTo(ProgramType.MAESTRIA);
        assertThat(afterMaestria.getStatus()).isEqualTo(ProgramStatus.EGRESADO);
        assertThat(afterDoctorado.getProgramType()).isEqualTo(ProgramType.DOCTORADO);
        assertThat(afterDoctorado.getStatus()).isEqualTo(ProgramStatus.EN_INVESTIGACION);
        assertThat(afterEspecializacion.getProgramType()).isEqualTo(ProgramType.ESPECIALIZACION);
        assertThat(afterEspecializacion.getStatus()).isEqualTo(ProgramStatus.EN_INVESTIGACION);
    }

    @Test
    @DisplayName("SPEC-044: rejects a second specialization row for the same student")
    void rejectsSecondSpecializationRow() {
        StudentProgram original = adapter.save(new StudentProgram(
                studentId, 1L, "2244556677", ProgramType.ESPECIALIZACION,
                LocalDate.of(2024, 1, 15), ProgramStatus.EN_INVESTIGACION));
        entityManager.flush();
        Long originalId = original.getId();
        entityManager.clear();

        assertThatThrownBy(() -> adapter.save(new StudentProgram(
                studentId, 1L, "2244556678", ProgramType.ESPECIALIZACION,
                LocalDate.of(2025, 1, 15), ProgramStatus.EN_INVESTIGACION)))
                .isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();
        StudentProgram remaining = entityManager.find(StudentProgram.class, originalId);
        assertThat(remaining).isNotNull();
        assertThat(remaining.getProgramType()).isEqualTo(ProgramType.ESPECIALIZACION);
        Long specializationRows = entityManager.getEntityManager()
                .createQuery(
                        "SELECT COUNT(sp) FROM StudentProgram sp WHERE sp.studentId = :studentId AND sp.programType = :programType",
                        Long.class)
                .setParameter("studentId", studentId)
                .setParameter("programType", ProgramType.ESPECIALIZACION)
                .getSingleResult();
        assertThat(specializationRows).isEqualTo(1L);
    }

    @Test
    @DisplayName("SPEC-037: detects tutor assignment on EN_INVESTIGACION program")
    void hasActiveAssignmentForEnInvestigacion() {
        StudentProgram program = new StudentProgram(
                studentId, 1L, "2123803361", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), ProgramStatus.EN_INVESTIGACION);
        program.updateMetadata(
                LocalDate.of(2023, 9, 1), null, null, null, ProgramStatus.EN_INVESTIGACION, null, 10L);
        adapter.save(program);
        entityManager.flush();

        assertThat(adapter.hasActiveAssignmentAsTutorOrAdvisor(10L)).isTrue();
        assertThat(adapter.hasActiveAssignmentAsTutorOrAdvisor(99L)).isFalse();
    }

    @Test
    @DisplayName("SPEC-037: ignores tutor assignment on BAJA program")
    void ignoresAssignmentOnBaja() {
        StudentProgram program = new StudentProgram(
                studentId, 1L, "2123803361", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), ProgramStatus.EN_INVESTIGACION);
        program.updateMetadata(
                LocalDate.of(2023, 9, 1), null, null, null, ProgramStatus.BAJA, "Abandono", 10L);
        adapter.save(program);
        entityManager.flush();

        assertThat(adapter.hasActiveAssignmentAsTutorOrAdvisor(10L)).isFalse();
    }

    private Long persistStudent(String enrollmentId, Long userId, ProgramType programType) {
        Student student = new Student(
                enrollmentId,
                userId,
                1L,
                null,
                new PersonalData("Emanuel", "López", "Alba", "Mexicana", LocalDate.of(1996, 4, 12), "5551002000", null),
                new AcademicInformation(
                        "Computación", DegreeLevel.LICENCIATURA, programType, LocalDate.of(2024, 1, 15), "24I"));
        entityManager.persist(student);
        entityManager.flush();
        return student.getId();
    }
}

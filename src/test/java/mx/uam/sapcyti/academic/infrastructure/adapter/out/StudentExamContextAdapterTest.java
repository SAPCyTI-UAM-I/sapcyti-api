package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.AcademicInformation;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.academic.domain.model.StudentExamContextOutcome;
import mx.uam.sapcyti.academic.domain.model.StudentExamContextResult;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentExamContextAdapterTest {

    private static final Long GRADUATE_PROGRAM_ID = 1L;

    @Mock
    private StudentRepositoryPort studentRepository;

    @Mock
    private StudentProgramRepositoryPort studentProgramRepository;

    @InjectMocks
    private StudentExamContextAdapter adapter;

    @Test
    @DisplayName("Resolve the exam context from the matching program row")
    void resolveMatchingProgramRow() {
        Student student = student(
                52L,
                520L,
                ProgramType.ESPECIALIZACION,
                DegreeLevel.LICENCIATURA,
                name("Emanuel", "López", "Alba"));
        StudentProgram program = program(
                102L,
                52L,
                ProgramType.ESPECIALIZACION,
                ProgramStatus.EN_INVESTIGACION,
                LocalDate.of(2024, 1, 15));
        givenStudent(student);
        givenPrograms(52L, program);

        StudentExamContextResult result = adapter.resolve(520L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.RESOLVED);
        assertThat(result.context()).isEqualTo(new StudentExamContext(
                52L,
                GRADUATE_PROGRAM_ID,
                102L,
                ProgramType.ESPECIALIZACION,
                ProgramStatus.EN_INVESTIGACION,
                "Emanuel",
                "López",
                "Alba"));
    }

    @Test
    @DisplayName("A resolved context may omit the maternal surname")
    void resolvedContextOmitsBlankMaternalSurname() {
        Student student = student(
                53L,
                530L,
                ProgramType.ESPECIALIZACION,
                DegreeLevel.LICENCIATURA,
                name("Emanuel", "López", "  "));
        givenStudent(student);
        givenPrograms(
                53L,
                program(
                        103L,
                        53L,
                        ProgramType.ESPECIALIZACION,
                        ProgramStatus.EN_INVESTIGACION,
                        LocalDate.of(2024, 1, 15)));

        StudentExamContextResult result = adapter.resolve(530L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.RESOLVED);
        assertThat(result.context().secondLastName()).isNull();
    }

    @Test
    @DisplayName("No student record for the authenticated user")
    void noStudentRecord() {
        when(studentRepository.findByUserIdAndGraduateProgramId(999L, GRADUATE_PROGRAM_ID))
                .thenReturn(Optional.empty());

        StudentExamContextResult result = adapter.resolve(999L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.NOT_FOUND);
        assertThat(result.context()).isNull();
        verifyNoInteractions(studentProgramRepository);
    }

    @Test
    @DisplayName("No coherent program row is ambiguous")
    void noCoherentProgramRowIsAmbiguous() {
        Student student = student(
                54L,
                540L,
                ProgramType.MAESTRIA,
                DegreeLevel.LICENCIATURA,
                name("Emanuel", "López", "Alba"));
        givenStudent(student);
        givenPrograms(
                54L,
                program(
                        200L,
                        54L,
                        ProgramType.DOCTORADO,
                        ProgramStatus.EN_INVESTIGACION,
                        LocalDate.of(2020, 1, 15)));

        StudentExamContextResult result = adapter.resolve(540L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.AMBIGUOUS);
        assertThat(result.context()).isNull();
    }

    @Test
    @DisplayName("Two coherent program rows are ambiguous")
    void twoCoherentProgramRowsAreAmbiguous() {
        Student student = student(
                55L,
                550L,
                ProgramType.DOCTORADO,
                DegreeLevel.MAESTRIA,
                name("Emanuel", "López", "Alba"));
        givenStudent(student);
        givenPrograms(
                55L,
                program(201L, 55L, ProgramType.DOCTORADO, ProgramStatus.EGRESADO, LocalDate.of(2018, 9, 1)),
                program(
                        202L,
                        55L,
                        ProgramType.DOCTORADO,
                        ProgramStatus.EN_INVESTIGACION,
                        LocalDate.of(2024, 1, 15)));

        StudentExamContextResult result = adapter.resolve(550L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.AMBIGUOUS);
        assertThat(result.context()).isNull();
    }

    @Test
    @DisplayName("Official given name or paternal surname is missing")
    void missingGivenNameIsIncomplete() {
        Student student = student(
                56L,
                560L,
                ProgramType.ESPECIALIZACION,
                DegreeLevel.LICENCIATURA,
                name(" ", "López", "Alba"));
        givenStudent(student);
        givenPrograms(
                56L,
                program(
                        104L,
                        56L,
                        ProgramType.ESPECIALIZACION,
                        ProgramStatus.EN_INVESTIGACION,
                        LocalDate.of(2024, 1, 15)));

        StudentExamContextResult result = adapter.resolve(560L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.INCOMPLETE);
        assertThat(result.context()).isNull();
    }

    @Test
    @DisplayName("Official paternal surname is missing")
    void missingPaternalSurnameIsIncomplete() {
        Student student = student(
                56L,
                561L,
                ProgramType.ESPECIALIZACION,
                DegreeLevel.LICENCIATURA,
                name("Emanuel", "", "Alba"));
        givenStudent(student);
        givenPrograms(
                56L,
                program(
                        104L,
                        56L,
                        ProgramType.ESPECIALIZACION,
                        ProgramStatus.EN_INVESTIGACION,
                        LocalDate.of(2024, 1, 15)));

        StudentExamContextResult result = adapter.resolve(561L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.INCOMPLETE);
        assertThat(result.context()).isNull();
    }

    @Test
    @DisplayName("Prior degree is not the graduate program type")
    void priorDegreeIsNotTheProgramType() {
        Student student = student(
                57L,
                570L,
                ProgramType.ESPECIALIZACION,
                DegreeLevel.MAESTRIA,
                name("Emanuel", "López", "Alba"));
        givenStudent(student);
        givenPrograms(
                57L,
                program(105L, 57L, ProgramType.ESPECIALIZACION, ProgramStatus.EGRESADO, LocalDate.of(2024, 1, 15)));

        StudentExamContextResult result = adapter.resolve(570L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.RESOLVED);
        assertThat(result.context().programType()).isEqualTo(ProgramType.ESPECIALIZACION);
        assertThat(result.context().programStatus()).isEqualTo(ProgramStatus.EGRESADO);
    }

    @Test
    @DisplayName("Stored program type outside the enum is DEGREE_UNSUPPORTED")
    void storedProgramTypeOutsideEnum() {
        when(studentRepository.findByUserIdAndGraduateProgramId(520L, GRADUATE_PROGRAM_ID))
                .thenThrow(unrecognized(ProgramType.class));

        StudentExamContextResult result = adapter.resolve(520L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.DEGREE_UNSUPPORTED);
        assertThat(result.context()).isNull();
        verifyNoInteractions(studentProgramRepository);
    }

    @Test
    @DisplayName("Stored program status outside the enum is STATUS_UNSUPPORTED")
    void storedProgramStatusOutsideEnum() {
        Student student = student(
                52L,
                520L,
                ProgramType.ESPECIALIZACION,
                DegreeLevel.LICENCIATURA,
                name("Emanuel", "López", "Alba"));
        givenStudent(student);
        when(studentProgramRepository.findByStudentIdAndGraduateProgramId(52L, GRADUATE_PROGRAM_ID))
                .thenThrow(unrecognized(ProgramStatus.class));

        StudentExamContextResult result = adapter.resolve(520L, GRADUATE_PROGRAM_ID);

        assertThat(result.outcome()).isEqualTo(StudentExamContextOutcome.STATUS_UNSUPPORTED);
        assertThat(result.context()).isNull();
    }

    @Test
    @DisplayName("An unknown prior degree is not reported as DEGREE_UNSUPPORTED")
    void unknownPriorDegreePropagates() {
        when(studentRepository.findByUserIdAndGraduateProgramId(520L, GRADUATE_PROGRAM_ID))
                .thenThrow(unrecognized(DegreeLevel.class));

        assertThatThrownBy(() -> adapter.resolve(520L, GRADUATE_PROGRAM_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Could not extract column")
                .cause()
                .hasMessageContaining(DegreeLevel.class.getName());
    }

    private void givenStudent(Student student) {
        when(studentRepository.findByUserIdAndGraduateProgramId(student.getUserId(), GRADUATE_PROGRAM_ID))
                .thenReturn(Optional.of(student));
    }

    private void givenPrograms(Long studentId, StudentProgram... programs) {
        when(studentProgramRepository.findByStudentIdAndGraduateProgramId(studentId, GRADUATE_PROGRAM_ID))
                .thenReturn(List.of(programs));
    }

    private static RuntimeException unrecognized(Class<? extends Enum<?>> enumType) {
        IllegalArgumentException cause =
                new IllegalArgumentException("No enum constant " + enumType.getName() + ".LEGACY");
        return new RuntimeException("Could not extract column", cause);
    }

    private static Student student(
            Long id,
            Long userId,
            ProgramType programType,
            DegreeLevel lastDegreeObtained,
            PersonalData personalData) {
        Student student = new Student(
                "2244556677",
                userId,
                GRADUATE_PROGRAM_ID,
                null,
                personalData,
                new AcademicInformation(
                        "Computación",
                        lastDegreeObtained,
                        programType,
                        LocalDate.of(2024, 1, 15),
                        "24I"));
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private static PersonalData name(String firstName, String firstLastName, String secondLastName) {
        return new PersonalData(
                firstName,
                firstLastName,
                secondLastName,
                "Mexicana",
                LocalDate.of(1996, 4, 12),
                "5551002000",
                null);
    }

    private static StudentProgram program(
            Long id,
            Long studentId,
            ProgramType programType,
            ProgramStatus status,
            LocalDate admissionDate) {
        StudentProgram program = new StudentProgram(
                studentId,
                GRADUATE_PROGRAM_ID,
                "2244556677",
                programType,
                admissionDate,
                status);
        ReflectionTestUtils.setField(program, "id", id);
        return program;
    }
}

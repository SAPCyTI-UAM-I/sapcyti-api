package mx.uam.sapcyti.academic.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.exception.StudentProgramNotFoundException;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorInformation;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GetStudentProgramUseCaseTest {

    @Mock private StudentRepositoryPort studentRepository;
    @Mock private StudentProgramRepositoryPort studentProgramRepository;
    @Mock private ProfessorRepositoryPort professorRepository;

    @InjectMocks
    private GetStudentProgramUseCase useCase;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("HU-19: returns program details with resolved tutor and advisors")
    void viewProgramDetails() {
        Student student = sampleStudent(50L);
        StudentProgram program = sampleProgram(100L, 50L);
        program.replaceAdvisors(List.of(11L));
        ReflectionTestUtils.setField(program, "tutorId", 10L);

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(studentProgramRepository.findByIdAndStudentIdAndGraduateProgramId(100L, 50L, 1L))
                .thenReturn(Optional.of(program));
        when(professorRepository.findById(10L)).thenReturn(Optional.of(sampleProfessor(10L, "Humberto", "Cervantes", "Maceda")));
        when(professorRepository.findById(11L)).thenReturn(Optional.of(sampleProfessor(11L, "Manuel", "Aguilar", "Cornejo")));

        GetStudentProgramUseCase.StudentProgramDetail detail = useCase.execute(50L, 100L);

        assertThat(detail.id()).isEqualTo(100L);
        assertThat(detail.enrollmentId()).isEqualTo("2123803361");
        assertThat(detail.programType()).isEqualTo(ProgramType.MAESTRIA);
        assertThat(detail.tutor().id()).isEqualTo(10L);
        assertThat(detail.advisors()).hasSize(1);
        assertThat(detail.advisors().getFirst().id()).isEqualTo(11L);
    }

    @Test
    @DisplayName("HU-19: program without tutor or advisors")
    void viewProgramWithoutAssignments() {
        Student student = sampleStudent(50L);
        StudentProgram program = sampleProgram(100L, 50L);

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(studentProgramRepository.findByIdAndStudentIdAndGraduateProgramId(100L, 50L, 1L))
                .thenReturn(Optional.of(program));

        GetStudentProgramUseCase.StudentProgramDetail detail = useCase.execute(50L, 100L);

        assertThat(detail.tutorId()).isNull();
        assertThat(detail.tutor()).isNull();
        assertThat(detail.advisorIds()).isEmpty();
    }

    @Test
    @DisplayName("HU-44: legacy free-text research area maps to unclassified on read")
    void legacyUnclassified() {
        Student student = sampleStudent(50L);
        StudentProgram program = sampleProgram(100L, 50L);
        ReflectionTestUtils.setField(program, "researchArea", "Machine Learning aplicado");

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(studentProgramRepository.findByIdAndStudentIdAndGraduateProgramId(100L, 50L, 1L))
                .thenReturn(Optional.of(program));

        GetStudentProgramUseCase.StudentProgramDetail detail = useCase.execute(50L, 100L);

        assertThat(detail.lineOfKnowledge()).isEqualTo(StudentProgram.UNCLASSIFIED);
        assertThat(detail.researchArea()).isEqualTo(StudentProgram.UNCLASSIFIED);
    }

    @Test
    @DisplayName("HU-19: non-existent program returns not found")
    void programNotFound() {
        when(studentRepository.findById(50L)).thenReturn(Optional.of(sampleStudent(50L)));
        when(studentProgramRepository.findByIdAndStudentIdAndGraduateProgramId(999L, 50L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(50L, 999L))
                .isInstanceOf(StudentProgramNotFoundException.class);
    }

    @Test
    @DisplayName("HU-19: program outside tenant scope returns not found")
    void programOutsideTenant() {
        Student student = new Student(
                "2123803361", 500L, 2L, null,
                new PersonalData("Paulina", "Valencia", "Franco", "Mexicana", LocalDate.of(1998, 3, 15), "5554821234", null),
                new mx.uam.sapcyti.academic.domain.model.AcademicInformation(
                        "Computación", "Licenciatura", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1)));
        ReflectionTestUtils.setField(student, "id", 50L);

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> useCase.execute(50L, 100L))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    @DisplayName("rejects missing tenant scope")
    void missingTenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> useCase.execute(50L, 100L))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    private static Student sampleStudent(Long id) {
        Student student = new Student(
                "2123803361", 500L, 1L, null,
                new PersonalData("Paulina", "Valencia", "Franco", "Mexicana", LocalDate.of(1998, 3, 15), "5554821234", null),
                new mx.uam.sapcyti.academic.domain.model.AcademicInformation(
                        "Computación", "Licenciatura", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1)));
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private static StudentProgram sampleProgram(Long id, Long studentId) {
        StudentProgram program = new StudentProgram(
                studentId, 1L, "2123803361", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), ProgramStatus.ACTIVO);
        ReflectionTestUtils.setField(program, "id", id);
        return program;
    }

    private static Professor sampleProfessor(Long id, String firstName, String firstLastName, String secondLastName) {
        Professor professor = new Professor(
                "EMP" + id,
                id * 10,
                1L,
                new PersonalData(firstName, firstLastName, secondLastName, null, null, "5554820000", null),
                new ProfessorInformation(false, null, null));
        ReflectionTestUtils.setField(professor, "id", id);
        return professor;
    }
}

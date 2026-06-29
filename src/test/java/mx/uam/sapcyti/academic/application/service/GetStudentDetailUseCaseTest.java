package mx.uam.sapcyti.academic.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
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
class GetStudentDetailUseCaseTest {

    @Mock private StudentRepositoryPort studentRepository;
    @Mock private StudentProgramRepositoryPort studentProgramRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private GetStudentProgramUseCase getStudentProgramUseCase;

    @InjectMocks
    private GetStudentDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("HU-17: returns unified detail with embedded program")
    void unifiedDetail() {
        Student student = sampleStudent();
        StudentProgram program = sampleProgram();
        User user = new User("paulina.valencia@uam.mx", "hash", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(user, "id", 500L);

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(userRepository.findById(500L)).thenReturn(Optional.of(user));
        when(studentProgramRepository.findByStudentIdAndGraduateProgramId(50L, 1L))
                .thenReturn(List.of(program));
        when(getStudentProgramUseCase.execute(50L, 100L)).thenReturn(sampleProgramDetail());

        GetStudentDetailUseCase.StudentDetail detail = useCase.execute(50L);

        assertThat(detail.student().getEnrollmentId()).isEqualTo("2123803361");
        assertThat(detail.program()).isNotNull();
        assertThat(detail.program().id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("HU-44: legacy research area shows as unclassified in embedded program")
    void legacyUnclassified() {
        Student student = sampleStudent();
        User user = new User("paulina.valencia@uam.mx", "hash", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(user, "id", 500L);
        StudentProgram program = sampleProgram();
        ReflectionTestUtils.setField(program, "researchArea", "Machine Learning aplicado");

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(userRepository.findById(500L)).thenReturn(Optional.of(user));
        when(studentProgramRepository.findByStudentIdAndGraduateProgramId(50L, 1L))
                .thenReturn(List.of(program));
        when(getStudentProgramUseCase.execute(50L, 100L)).thenReturn(
                new GetStudentProgramUseCase.StudentProgramDetail(
                        100L, 50L, 1L, "2123803361", ProgramType.MAESTRIA,
                        LocalDate.of(2023, 9, 1), null,
                        StudentProgram.UNCLASSIFIED, StudentProgram.UNCLASSIFIED,
                        ProgramStatus.ACTIVO, null, null, null, List.of(), List.of()));

        GetStudentDetailUseCase.StudentDetail detail = useCase.execute(50L);

        assertThat(detail.program().lineOfKnowledge()).isEqualTo(StudentProgram.UNCLASSIFIED);
        assertThat(detail.program().researchArea()).isEqualTo(StudentProgram.UNCLASSIFIED);
    }

    private static GetStudentProgramUseCase.StudentProgramDetail sampleProgramDetail() {
        return new GetStudentProgramUseCase.StudentProgramDetail(
                100L, 50L, 1L, "2123803361", ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1), null, null, null,
                ProgramStatus.ACTIVO, null, null, null, List.of(), List.of());
    }

    private static Student sampleStudent() {
        Student student = new Student(
                "2123803361", 500L, 1L, null,
                new PersonalData("Paulina", "Valencia", "Franco", "Mexicana", LocalDate.of(1998, 3, 15), "5554821234", null),
                new mx.uam.sapcyti.academic.domain.model.AcademicInformation(
                        "Computación", "Licenciatura", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1)));
        ReflectionTestUtils.setField(student, "id", 50L);
        return student;
    }

    private static StudentProgram sampleProgram() {
        StudentProgram program = new StudentProgram(
                50L, 1L, "2123803361", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), ProgramStatus.ACTIVO);
        ReflectionTestUtils.setField(program, "id", 100L);
        return program;
    }
}

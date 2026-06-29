package mx.uam.sapcyti.academic.application.service;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.internoProfessor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.application.command.UpdateStudentProgramCommand;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.exception.StudentProgramNotFoundException;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.academic.domain.service.ResearchCatalogValidator;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UpdateStudentProgramUseCaseTest {

    @Mock private StudentRepositoryPort studentRepository;
    @Mock private StudentProgramRepositoryPort studentProgramRepository;
    @Mock private ProfessorRepositoryPort professorRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private GetStudentProgramUseCase getStudentProgramUseCase;
    @Mock private ResearchCatalogValidator researchCatalogValidator;

    @InjectMocks
    private UpdateStudentProgramUseCase useCase;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("HU-20: assigns tutor to student program")
    void assignTutor() {
        stubStudentAndProgram();
        stubActiveProfessor(10L);
        when(studentProgramRepository.save(any(StudentProgram.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(getStudentProgramUseCase.execute(50L, 100L)).thenReturn(sampleDetail(10L, List.of()));

        UpdateStudentProgramCommand command = command(10L, List.of(), null, null, null, null, null);

        GetStudentProgramUseCase.StudentProgramDetail result = useCase.execute(command);

        assertThat(result.tutorId()).isEqualTo(10L);
        ArgumentCaptor<StudentProgram> captor = ArgumentCaptor.forClass(StudentProgram.class);
        verify(studentProgramRepository).save(captor.capture());
        assertThat(captor.getValue().getTutorId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("HU-20: assigns multiple advisors")
    void assignAdvisors() {
        stubStudentAndProgram();
        stubActiveProfessor(10L);
        stubActiveProfessor(11L);
        when(studentProgramRepository.save(any(StudentProgram.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(getStudentProgramUseCase.execute(50L, 100L)).thenReturn(sampleDetail(null, List.of(10L, 11L)));

        UpdateStudentProgramCommand command = command(null, List.of(10L, 11L), null, null, null, null, null);

        useCase.execute(command);

        ArgumentCaptor<StudentProgram> captor = ArgumentCaptor.forClass(StudentProgram.class);
        verify(studentProgramRepository).save(captor.capture());
        assertThat(captor.getValue().getAdvisorIds()).containsExactly(10L, 11L);
    }

    @Test
    @DisplayName("HU-20: records program withdrawal with reason")
    void recordWithdrawal() {
        stubStudentAndProgram();
        when(studentProgramRepository.save(any(StudentProgram.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(getStudentProgramUseCase.execute(50L, 100L)).thenReturn(sampleDetail(null, List.of()));

        UpdateStudentProgramCommand command = command(null, List.of(), ProgramStatus.BAJA, "Abandono", null, null, null);

        useCase.execute(command);

        ArgumentCaptor<StudentProgram> captor = ArgumentCaptor.forClass(StudentProgram.class);
        verify(studentProgramRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProgramStatus.BAJA);
        assertThat(captor.getValue().getWithdrawalReason()).isEqualTo("Abandono");
    }

    @Test
    @DisplayName("HU-20: clears tutor assignment")
    void clearTutor() {
        StudentProgram program = sampleProgram();
        ReflectionTestUtils.setField(program, "tutorId", 10L);
        stubStudentAndProgram(program);
        when(studentProgramRepository.save(any(StudentProgram.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(getStudentProgramUseCase.execute(50L, 100L)).thenReturn(sampleDetail(null, List.of()));

        UpdateStudentProgramCommand command = command(null, List.of(), null, null, null, null, null);

        useCase.execute(command);

        ArgumentCaptor<StudentProgram> captor = ArgumentCaptor.forClass(StudentProgram.class);
        verify(studentProgramRepository).save(captor.capture());
        assertThat(captor.getValue().getTutorId()).isNull();
    }

    @Test
    @DisplayName("HU-20: rejects non-existent tutor")
    void unknownTutor() {
        stubStudentAndProgram();
        when(professorRepository.findByIdAndGraduateProgramId(999L, 1L)).thenReturn(Optional.empty());

        UpdateStudentProgramCommand command = command(999L, List.of(), null, null, null, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ProfessorNotFoundException.class);
    }

    @Test
    @DisplayName("HU-20: rejects non-existent advisor")
    void unknownAdvisor() {
        stubStudentAndProgram();
        when(professorRepository.findByIdAndGraduateProgramId(999L, 1L)).thenReturn(Optional.empty());

        UpdateStudentProgramCommand command = command(null, List.of(999L), null, null, null, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ProfessorNotFoundException.class);
    }

    @Test
    @DisplayName("HU-20: rejects duplicate advisors in request")
    void duplicateAdvisors() {
        stubStudentAndProgram();

        UpdateStudentProgramCommand command = command(null, List.of(10L, 10L), null, null, null, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate advisor IDs");
    }

    @Test
    @DisplayName("HU-20: rejects graduation date before admission date")
    void invalidGraduationDate() {
        stubStudentAndProgram();

        UpdateStudentProgramCommand command = command(
                null, List.of(), null, null, LocalDate.of(2024, 1, 1), LocalDate.of(2023, 12, 31), null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Graduation date");
    }

    @Test
    @DisplayName("HU-20: rejects BAJA without withdrawal reason")
    void missingWithdrawalReason() {
        stubStudentAndProgram();

        UpdateStudentProgramCommand command = command(null, List.of(), ProgramStatus.BAJA, " ", null, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Withdrawal reason");
    }

    @Test
    @DisplayName("HU-20: rejects non-existent program")
    void programNotFound() {
        when(studentRepository.findById(50L)).thenReturn(Optional.of(sampleStudent()));
        when(studentProgramRepository.findByIdAndStudentIdAndGraduateProgramId(999L, 50L, 1L))
                .thenReturn(Optional.empty());

        UpdateStudentProgramCommand command = command(null, List.of(), null, null, null, null, 999L);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(StudentProgramNotFoundException.class);
    }

    private void stubActiveProfessor(Long professorId) {
        Professor professor = internoProfessor("EMP" + professorId, professorId * 10, 1L,
                new PersonalData("Prof", "Test", null, null, null, "5554820000", null));
        ReflectionTestUtils.setField(professor, "id", professorId);
        when(professorRepository.findByIdAndGraduateProgramId(professorId, 1L))
                .thenReturn(Optional.of(professor));
        User user = new User("prof" + professorId + "@uam.mx", "hash", RoleType.PROFESSOR, 1L);
        ReflectionTestUtils.setField(user, "id", professorId * 10);
        when(userRepository.findById(professorId * 10)).thenReturn(Optional.of(user));
    }

    private void stubStudentAndProgram() {
        stubStudentAndProgram(sampleProgram());
    }

    private void stubStudentAndProgram(StudentProgram program) {
        when(studentRepository.findById(50L)).thenReturn(Optional.of(sampleStudent()));
        when(studentProgramRepository.findByIdAndStudentIdAndGraduateProgramId(100L, 50L, 1L))
                .thenReturn(Optional.of(program));
    }

    private static UpdateStudentProgramCommand command(
            Long tutorId,
            List<Long> advisorIds,
            ProgramStatus status,
            String withdrawalReason,
            LocalDate admissionDate,
            LocalDate graduationDate,
            Long programId) {
        return new UpdateStudentProgramCommand(
                50L,
                programId != null ? programId : 100L,
                admissionDate != null ? admissionDate : LocalDate.of(2023, 9, 1),
                graduationDate,
                null,
                null,
                status != null ? status : ProgramStatus.ACTIVO,
                withdrawalReason,
                tutorId,
                advisorIds != null ? advisorIds : List.of());
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

    private static GetStudentProgramUseCase.StudentProgramDetail sampleDetail(Long tutorId, List<Long> advisorIds) {
        return new GetStudentProgramUseCase.StudentProgramDetail(
                100L, 50L, 1L, "2123803361", ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1), null, null, null, ProgramStatus.ACTIVO, null,
                tutorId, null, advisorIds, List.of());
    }
}

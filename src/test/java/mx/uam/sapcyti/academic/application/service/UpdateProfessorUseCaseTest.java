package mx.uam.sapcyti.academic.application.service;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.internoProfessor;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.professorPersonalData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.application.command.UpdateProfessorCommand;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEmployeeNumberException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateProfessorEmailException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UpdateProfessorUseCaseTest {

    @Mock private ProfessorRepositoryPort professorRepository;
    @Mock private UserRepositoryPort userRepository;

    private UpdateProfessorUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateProfessorUseCase(professorRepository, userRepository);
        TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("updates professor and linked user email")
    void happyPath() {
        Professor professor = sampleProfessor(10L, ProfessorType.INTERNO, "30568", 20L);
        User user = sampleUser(20L, "humberto.cervantes@uam.mx");
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(1L, "30568"))
                .thenReturn(List.of(professor));
        when(userRepository.findByEmail("humberto.nuevo@uam.mx")).thenReturn(Optional.empty());
        when(professorRepository.save(any(Professor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListProfessorsUseCase.ProfessorListItem result = useCase.execute(sampleCommand());

        assertThat(result.getEmail()).isEqualTo("humberto.nuevo@uam.mx");
        assertThat(result.getProfessorType()).isEqualTo(ProfessorType.INTERNO);
        verify(userRepository).save(user);
        assertThat(user.getEmail()).isEqualTo("humberto.nuevo@uam.mx");
    }

    @Test
    @DisplayName("change interno to externo clears employee number")
    void internoToExterno() {
        Professor professor = sampleProfessor(10L, ProfessorType.INTERNO, "30568", 20L);
        User user = sampleUser(20L, "humberto.cervantes@uam.mx");
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("humberto.cervantes@uam.mx")).thenReturn(Optional.of(user));
        when(professorRepository.save(any(Professor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfessorCommand command = new UpdateProfessorCommand(
                10L,
                ProfessorType.EXTERNO,
                null,
                "humberto.cervantes@uam.mx",
                "Humberto",
                "Cervantes",
                "Maceda",
                "5554825678",
                null,
                false,
                null,
                null);

        ListProfessorsUseCase.ProfessorListItem result = useCase.execute(command);

        assertThat(result.getProfessorType()).isEqualTo(ProfessorType.EXTERNO);
        assertThat(result.getEmployeeNumber()).isNull();
    }

    @Test
    @DisplayName("rejects interno without employee number")
    void internoWithoutEmployeeNumber() {
        Professor professor = sampleProfessor(10L, ProfessorType.EXTERNO, null, 20L);
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(sampleUser(20L, "externo@uam.mx")));

        assertThatThrownBy(() -> useCase.execute(new UpdateProfessorCommand(
                        10L, ProfessorType.INTERNO, null, "externo@uam.mx",
                        "Juan", "Perez", null, "5554825678", null, false, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Employee number is required for internal professors");
    }

    @Test
    @DisplayName("rejects duplicate email")
    void duplicateEmail() {
        Professor professor = sampleProfessor(10L, ProfessorType.INTERNO, "30568", 20L);
        User user = sampleUser(20L, "humberto.cervantes@uam.mx");
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(1L, "30568"))
                .thenReturn(List.of(professor));
        when(userRepository.findByEmail("existing@uam.mx"))
                .thenReturn(Optional.of(sampleUser(99L, "existing@uam.mx")));

        UpdateProfessorCommand command = new UpdateProfessorCommand(
                10L, ProfessorType.INTERNO, "30568", "existing@uam.mx",
                "Humberto", "Cervantes", null, "5554825678", null, false, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DuplicateProfessorEmailException.class);
    }

    @Test
    @DisplayName("rejects duplicate employee number")
    void duplicateEmployeeNumber() {
        Professor professor = sampleProfessor(10L, ProfessorType.INTERNO, "30568", 20L);
        Professor other = sampleProfessor(11L, ProfessorType.INTERNO, "30568", 30L);
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(sampleUser(20L, "humberto.cervantes@uam.mx")));
        when(professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(1L, "40100"))
                .thenReturn(List.of(other));
        when(userRepository.findById(30L)).thenReturn(Optional.of(sampleUser(30L, "other@uam.mx")));

        UpdateProfessorCommand command = new UpdateProfessorCommand(
                10L, ProfessorType.INTERNO, "40100", "humberto.cervantes@uam.mx",
                "Humberto", "Cervantes", null, "5554825678", null, false, null, null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DuplicateEmployeeNumberException.class);
    }

    @Test
    @DisplayName("professor not found in tenant")
    void professorNotFound() {
        when(professorRepository.findByIdAndGraduateProgramId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateProfessorCommand(
                        999L, ProfessorType.INTERNO, "30568", "humberto.cervantes@uam.mx",
                        "Humberto", "Cervantes", null, "5554825678", null, false, null, null)))
                .isInstanceOf(ProfessorNotFoundException.class);
    }

    private static UpdateProfessorCommand sampleCommand() {
        return new UpdateProfessorCommand(
                10L,
                ProfessorType.INTERNO,
                "30568",
                "humberto.nuevo@uam.mx",
                "Humberto Gustavo",
                "Cervantes",
                "Maceda",
                "5559998877",
                "1234",
                false,
                LocalDate.of(2028, 1, 1),
                LocalDate.of(2028, 6, 30));
    }

    private static Professor sampleProfessor(
            Long id, ProfessorType type, String employeeNumber, Long userId) {
        Professor professor = internoProfessor(employeeNumber, userId, 1L, professorPersonalData());
        professor.updateTypeAndEmployeeNumber(type, employeeNumber);
        ReflectionTestUtils.setField(professor, "id", id);
        return professor;
    }

    private static User sampleUser(Long id, String email) {
        User user = new User(email, "hash", RoleType.PROFESSOR, 1L);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

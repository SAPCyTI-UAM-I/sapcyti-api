package mx.uam.sapcyti.academic.application.service;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.internoProfessor;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.professorPersonalData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.exception.ProfessorAlreadyInactiveException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorHasActiveAssignmentsException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorTrimestralAssignmentsPort;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorTrimestralAssignmentsPort.OpenGroupAssignment;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
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
class DeactivateProfessorUseCaseTest {

    @Mock private ProfessorRepositoryPort professorRepository;
    @Mock private StudentProgramRepositoryPort studentProgramRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private ProfessorTrimestralAssignmentsPort trimestralAssignmentsPort;

    private DeactivateProfessorUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeactivateProfessorUseCase(
                professorRepository,
                studentProgramRepository,
                userRepository,
                trimestralAssignmentsPort);
        TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("deactivates professor when no active assignments")
    void happyPath() {
        Professor professor = sampleProfessor(10L, 20L);
        User user = sampleUser(20L, true);
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(studentProgramRepository.hasActiveAssignmentAsTutorOrAdvisor(10L)).thenReturn(false);
        when(trimestralAssignmentsPort.findOpenGroupAssignments(10L, 1L)).thenReturn(List.of());

        ListProfessorsUseCase.ProfessorListItem result = useCase.execute(10L);

        assertThat(result.isActive()).isFalse();
        verify(userRepository).save(user);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("rejects when professor has active assignments")
    void activeAssignments() {
        Professor professor = sampleProfessor(10L, 20L);
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(sampleUser(20L, true)));
        when(studentProgramRepository.hasActiveAssignmentAsTutorOrAdvisor(10L)).thenReturn(true);
        when(trimestralAssignmentsPort.findOpenGroupAssignments(10L, 1L)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(10L))
                .isInstanceOfSatisfying(
                        ProfessorHasActiveAssignmentsException.class,
                        exception -> {
                            assertThat(exception.hasTutorOrAdvisorAssignments()).isTrue();
                            assertThat(exception.getOpenGroupAssignments()).isEmpty();
                        });
    }

    @Test
    @DisplayName("reports every open trimestral group assignment")
    void openGroupAssignments() {
        Professor professor = sampleProfessor(10L, 20L);
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L))
                .thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(sampleUser(20L, true)));
        when(studentProgramRepository.hasActiveAssignmentAsTutorOrAdvisor(10L)).thenReturn(false);
        List<OpenGroupAssignment> assignments = List.of(
                new OpenGroupAssignment(30L, "26O", 40L, "2156041", "CO43"),
                new OpenGroupAssignment(31L, "27I", 41L, "2156042", "CI43"));
        when(trimestralAssignmentsPort.findOpenGroupAssignments(10L, 1L))
                .thenReturn(assignments);

        assertThatThrownBy(() -> useCase.execute(10L))
                .isInstanceOfSatisfying(
                        ProfessorHasActiveAssignmentsException.class,
                        exception -> {
                            assertThat(exception.hasTutorOrAdvisorAssignments()).isFalse();
                            assertThat(exception.getOpenGroupAssignments())
                                    .containsExactlyElementsOf(assignments);
                        });
    }

    @Test
    @DisplayName("rejects already inactive professor")
    void alreadyInactive() {
        Professor professor = sampleProfessor(10L, 20L);
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(sampleUser(20L, false)));

        assertThatThrownBy(() -> useCase.execute(10L))
                .isInstanceOf(ProfessorAlreadyInactiveException.class);
    }

    @Test
    @DisplayName("professor not found in tenant")
    void professorNotFound() {
        when(professorRepository.findByIdAndGraduateProgramId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(ProfessorNotFoundException.class);
    }

    private static Professor sampleProfessor(Long id, Long userId) {
        Professor professor = internoProfessor("30568", userId, 1L, professorPersonalData());
        ReflectionTestUtils.setField(professor, "id", id);
        return professor;
    }

    private static User sampleUser(Long id, boolean active) {
        User user = new User("humberto.cervantes@uam.mx", "hash", RoleType.PROFESSOR, 1L);
        ReflectionTestUtils.setField(user, "id", id);
        user.setActive(active);
        return user;
    }
}

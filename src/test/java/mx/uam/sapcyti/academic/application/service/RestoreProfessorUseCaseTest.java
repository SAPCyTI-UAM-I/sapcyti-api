package mx.uam.sapcyti.academic.application.service;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.internoProfessor;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.professorPersonalData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEmployeeNumberException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorAlreadyActiveException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Professor;
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
class RestoreProfessorUseCaseTest {

    @Mock private ProfessorRepositoryPort professorRepository;
    @Mock private UserRepositoryPort userRepository;

    private RestoreProfessorUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RestoreProfessorUseCase(professorRepository, userRepository);
        TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("restores inactive professor and reactivates linked user")
    void happyPath() {
        Professor professor = sampleProfessor(10L, 20L, "12345");
        User user = sampleUser(20L, false);
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(1L, "12345"))
                .thenReturn(List.of(professor));

        ListProfessorsUseCase.ProfessorListItem result = useCase.execute(10L);

        assertThat(result.isActive()).isTrue();
        assertThat(result.getEmployeeNumber()).isEqualTo("12345");
        verify(userRepository).save(user);
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("rejects already active professor")
    void alreadyActive() {
        Professor professor = sampleProfessor(10L, 20L, "12345");
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(sampleUser(20L, true)));

        assertThatThrownBy(() -> useCase.execute(10L))
                .isInstanceOf(ProfessorAlreadyActiveException.class);
    }

    @Test
    @DisplayName("rejects restore when NEMP collides with another active interno")
    void nempCollision() {
        Professor professor = sampleProfessor(10L, 20L, "12345");
        Professor other = sampleProfessor(11L, 30L, "12345");
        User user = sampleUser(20L, false);
        User otherUser = sampleUser(30L, true);
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.of(professor));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(1L, "12345"))
                .thenReturn(List.of(professor, other));
        when(userRepository.findById(30L)).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> useCase.execute(10L))
                .isInstanceOf(DuplicateEmployeeNumberException.class);
    }

    @Test
    @DisplayName("professor not found in tenant")
    void professorNotFound() {
        when(professorRepository.findByIdAndGraduateProgramId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(ProfessorNotFoundException.class);
    }

    private static Professor sampleProfessor(Long id, Long userId, String employeeNumber) {
        Professor professor = internoProfessor(employeeNumber, userId, 1L, professorPersonalData());
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

package mx.uam.sapcyti.academic.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import mx.uam.sapcyti.academic.application.command.UpdateStudentCommand;
import mx.uam.sapcyti.academic.domain.exception.DuplicateStudentEmailException;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UpdateStudentUseCaseTest {

    @Mock private StudentRepositoryPort studentRepository;
    @Mock private UserRepositoryPort userRepository;

    @InjectMocks
    private UpdateStudentUseCase useCase;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("HU-18: updates student and linked user")
    void updateStudent() {
        Student student = sampleStudent();
        User user = sampleUser();
        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(userRepository.findById(500L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("paulina.updated@uam.mx")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStudentCommand command = sampleCommand("paulina.updated@uam.mx", true);
        ListStudentsUseCase.StudentListItem result = useCase.execute(command);

        assertThat(result.getEmail()).isEqualTo("paulina.updated@uam.mx");
        assertThat(result.isActive()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("paulina.updated@uam.mx");
    }

    @Test
    @DisplayName("HU-18: deactivates student account")
    void deactivateStudent() {
        Student student = sampleStudent();
        User user = sampleUser();
        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(userRepository.findById(500L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("paulina.valencia@uam.mx")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListStudentsUseCase.StudentListItem result = useCase.execute(sampleCommand("paulina.valencia@uam.mx", false));

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("HU-18: rejects duplicate email")
    void duplicateEmail() {
        Student student = sampleStudent();
        User user = sampleUser();
        User other = new User("existing@uam.mx", "hash", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(other, "id", 999L);

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(userRepository.findById(500L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("existing@uam.mx")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> useCase.execute(sampleCommand("existing@uam.mx", true)))
                .isInstanceOf(DuplicateStudentEmailException.class);
    }

    @Test
    @DisplayName("HU-18: student not found in tenant")
    void studentNotFound() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(sampleCommandWithId(999L)))
                .isInstanceOf(StudentNotFoundException.class);
    }

    private static UpdateStudentCommand sampleCommand(String email, boolean active) {
        return sampleCommandWithId(50L, email, active);
    }

    private static UpdateStudentCommand sampleCommandWithId(Long studentId) {
        return sampleCommandWithId(studentId, "paulina.valencia@uam.mx", true);
    }

    private static UpdateStudentCommand sampleCommandWithId(Long studentId, String email, boolean active) {
        return new UpdateStudentCommand(
                studentId,
                "Paulina",
                "Valencia Franco",
                "Franco",
                email,
                "Mexicana",
                LocalDate.of(1998, 3, 15),
                "5559998877",
                "4321",
                "Ingeniería en Computación",
                DegreeLevel.LICENCIATURA,
                ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1),
                active);
    }

    private static Student sampleStudent() {
        Student student = new Student(
                "2123803361", 500L, 1L, null,
                new PersonalData("Paulina", "Valencia", "Franco", "Mexicana", LocalDate.of(1998, 3, 15), "5554821234", null),
                new mx.uam.sapcyti.academic.domain.model.AcademicInformation(
                        "Computación", DegreeLevel.LICENCIATURA, ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1)));
        ReflectionTestUtils.setField(student, "id", 50L);
        return student;
    }

    private static User sampleUser() {
        User user = new User("paulina.valencia@uam.mx", "hash", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(user, "id", 500L);
        return user;
    }
}

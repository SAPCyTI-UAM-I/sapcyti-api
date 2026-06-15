package mx.uam.sapcyti.academic.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import mx.uam.sapcyti.academic.application.command.RegisterStudentCommand;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEnrollmentIdException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateStudentEmailException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.model.AcademicInformation;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.academic.domain.service.PasswordGenerationService;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.PasswordEncoderPort;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegisterStudentUseCaseTest {

    @Mock private GraduateProgramRepositoryPort programRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private StudentRepositoryPort studentRepository;
    @Mock private ProfessorRepositoryPort professorRepository;
    @Mock private PasswordGenerationService passwordGenerationService;
    @Mock private PasswordEncoderPort passwordEncoder;

    private RegisterStudentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterStudentUseCase(
                programRepository,
                userRepository,
                studentRepository,
                professorRepository,
                passwordGenerationService,
                passwordEncoder);
        TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("registers student and user atomically")
    void happyPath() {
        RegisterStudentCommand command = sampleCommand();
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(professorRepository.existsByIdAndGraduateProgramId(10L, 1L)).thenReturn(true);
        when(userRepository.existsByEmail("paulina.valencia@uam.mx")).thenReturn(false);
        when(studentRepository.existsByEnrollmentId("2123803361")).thenReturn(false);
        when(passwordGenerationService.generatePassword()).thenReturn("Kx9#mP2vLq4!");
        when(passwordEncoder.encode("Kx9#mP2vLq4!")).thenReturn("hashed");

        User savedUser = new User("paulina.valencia@uam.mx", "hashed", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(savedUser, "id", 20L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Student savedStudent = new Student(
                "2123803361",
                20L,
                1L,
                10L,
                new PersonalData("Paulina", "Valencia", "Franco", "Mexicana"),
                new AcademicInformation("Computación", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1)));
        ReflectionTestUtils.setField(savedStudent, "id", 1L);
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

        RegisterStudentUseCase.RegisterStudentResult result = useCase.execute(command);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getGeneratedPassword()).isEqualTo("Kx9#mP2vLq4!");
        assertThat(result.getEmail()).isEqualTo("paulina.valencia@uam.mx");
        assertThat(result.getAdvisorId()).isEqualTo(10L);
        verify(userRepository).save(any(User.class));
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("rejects duplicate email")
    void duplicateEmail() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(professorRepository.existsByIdAndGraduateProgramId(10L, 1L)).thenReturn(true);
        when(userRepository.existsByEmail("paulina.valencia@uam.mx")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(DuplicateStudentEmailException.class);
    }

    @Test
    @DisplayName("rejects duplicate enrollment id")
    void duplicateEnrollmentId() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(professorRepository.existsByIdAndGraduateProgramId(10L, 1L)).thenReturn(true);
        when(userRepository.existsByEmail("paulina.valencia@uam.mx")).thenReturn(false);
        when(studentRepository.existsByEnrollmentId("2123803361")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(DuplicateEnrollmentIdException.class);
    }

    @Test
    @DisplayName("rejects unknown graduate program")
    void unknownProgram() {
        when(programRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(GraduateProgramNotFoundException.class);
    }

    @Test
    @DisplayName("rejects non-existent advisor")
    void unknownAdvisor() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(professorRepository.existsByIdAndGraduateProgramId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(ProfessorNotFoundException.class);
    }

    @Test
    @DisplayName("rejects tenant mismatch")
    void tenantMismatch() {
        RegisterStudentCommand command = new RegisterStudentCommand(
                "2123803361",
                "paulina.valencia@uam.mx",
                99L,
                10L,
                "Paulina",
                "Valencia",
                "Franco",
                "Mexicana",
                "Computación",
                ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    @DisplayName("allows registration without advisor")
    void withoutAdvisor() {
        RegisterStudentCommand command = new RegisterStudentCommand(
                "2123803361",
                "paulina.valencia@uam.mx",
                1L,
                null,
                "Paulina",
                "Valencia",
                "Franco",
                "Mexicana",
                "Computación",
                ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1));
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(userRepository.existsByEmail("paulina.valencia@uam.mx")).thenReturn(false);
        when(studentRepository.existsByEnrollmentId("2123803361")).thenReturn(false);
        when(passwordGenerationService.generatePassword()).thenReturn("Kx9#mP2vLq4!");
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        User savedUser = new User("paulina.valencia@uam.mx", "hashed", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(savedUser, "id", 20L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        Student savedStudent = new Student(
                "2123803361",
                20L,
                1L,
                null,
                new PersonalData("Paulina", "Valencia", "Franco", "Mexicana"),
                new AcademicInformation("Computación", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1)));
        ReflectionTestUtils.setField(savedStudent, "id", 1L);
        when(studentRepository.save(studentCaptor.capture())).thenReturn(savedStudent);

        RegisterStudentUseCase.RegisterStudentResult result = useCase.execute(command);

        assertThat(result.getAdvisorId()).isNull();
        assertThat(studentCaptor.getValue().getAdvisorId()).isNull();
    }

    @Test
    @DisplayName("allows registration without secondLastName")
    void withoutSecondLastName() {
        RegisterStudentCommand command = new RegisterStudentCommand(
                "2123803361",
                "paulina.valencia@uam.mx",
                1L,
                null,
                "Paulina",
                "Valencia",
                null,
                "Mexicana",
                "Computación",
                ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1));
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(userRepository.existsByEmail("paulina.valencia@uam.mx")).thenReturn(false);
        when(studentRepository.existsByEnrollmentId("2123803361")).thenReturn(false);
        when(passwordGenerationService.generatePassword()).thenReturn("Kx9#mP2vLq4!");
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        User savedUser = new User("paulina.valencia@uam.mx", "hashed", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(savedUser, "id", 20L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        Student savedStudent = new Student(
                "2123803361",
                20L,
                1L,
                null,
                new PersonalData("Paulina", "Valencia", null, "Mexicana"),
                new AcademicInformation("Computación", ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1)));
        ReflectionTestUtils.setField(savedStudent, "id", 1L);
        when(studentRepository.save(studentCaptor.capture())).thenReturn(savedStudent);

        RegisterStudentUseCase.RegisterStudentResult result = useCase.execute(command);

        assertThat(result.getSecondLastName()).isNull();
        assertThat(studentCaptor.getValue().getPersonalData().getSecondLastName()).isNull();
    }

    private static RegisterStudentCommand sampleCommand() {
        return new RegisterStudentCommand(
                "2123803361",
                "paulina.valencia@uam.mx",
                1L,
                10L,
                "Paulina",
                "Valencia",
                "Franco",
                "Mexicana",
                "Computación",
                ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1));
    }
}

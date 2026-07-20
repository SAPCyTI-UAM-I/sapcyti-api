package mx.uam.sapcyti.academic.application.service;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.internoProfessor;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.studentPersonalData;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.studentPersonalDataWithoutExtension;
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
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.academic.domain.service.PasswordGenerationService;
import mx.uam.sapcyti.academic.domain.service.ResearchCatalogValidator;
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
    @Mock private StudentProgramRepositoryPort studentProgramRepository;
    @Mock private ProfessorRepositoryPort professorRepository;
    @Mock private PasswordGenerationService passwordGenerationService;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private ResearchCatalogValidator researchCatalogValidator;

    private RegisterStudentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterStudentUseCase(
                programRepository,
                userRepository,
                studentRepository,
                studentProgramRepository,
                professorRepository,
                passwordGenerationService,
                passwordEncoder,
                researchCatalogValidator);
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
        stubActiveProfessor(10L);
        when(userRepository.existsByEmail("paulina.valencia@uam.mx")).thenReturn(false);
        when(studentRepository.existsByEnrollmentId("2123803361")).thenReturn(false);
        when(passwordGenerationService.generatePassword()).thenReturn("Kx9#mP2vLq4!");
        when(passwordEncoder.encode("Kx9#mP2vLq4!")).thenReturn("hashed");

        User savedUser = new User("paulina.valencia@uam.mx", "hashed", RoleType.STUDENT, 1L);
        ReflectionTestUtils.setField(savedUser, "id", 20L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Student savedStudent = new Student(
                "2123803361", 20L, 1L, 10L, studentPersonalData(), sampleAcademicInformation());
        ReflectionTestUtils.setField(savedStudent, "id", 1L);
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);
        when(studentProgramRepository.save(any(StudentProgram.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterStudentUseCase.RegisterStudentResult result = useCase.execute(command);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getGeneratedPassword()).isEqualTo("Kx9#mP2vLq4!");
        assertThat(result.getEmail()).isEqualTo("paulina.valencia@uam.mx");
        assertThat(result.getAdvisorId()).isEqualTo(10L);
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1998, 3, 15));
        assertThat(result.getLastDegreeObtained()).isEqualTo(DegreeLevel.LICENCIATURA);
        verify(userRepository).save(any(User.class));
        verify(studentRepository).save(any(Student.class));
        verify(studentProgramRepository).save(any(StudentProgram.class));
    }

    @Test
    @DisplayName("rejects duplicate email")
    void duplicateEmail() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        stubActiveProfessor(10L);
        when(userRepository.existsByEmail("paulina.valencia@uam.mx")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(DuplicateStudentEmailException.class);
    }

    @Test
    @DisplayName("rejects duplicate enrollment id")
    void duplicateEnrollmentId() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        stubActiveProfessor(10L);
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
        when(professorRepository.findByIdAndGraduateProgramId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(ProfessorNotFoundException.class);
    }

    @Test
    @DisplayName("rejects tenant mismatch")
    void tenantMismatch() {
        RegisterStudentCommand command = sampleCommandBuilder().graduateProgramId(99L).build();

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    @DisplayName("allows registration without advisor")
    void withoutAdvisor() {
        RegisterStudentCommand command = sampleCommandBuilder().advisorId(null).phoneExtension(null).build();
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
                "2123803361", 20L, 1L, null, studentPersonalData(), sampleAcademicInformation());
        ReflectionTestUtils.setField(savedStudent, "id", 1L);
        when(studentRepository.save(studentCaptor.capture())).thenReturn(savedStudent);
        when(studentProgramRepository.save(any(StudentProgram.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterStudentUseCase.RegisterStudentResult result = useCase.execute(command);

        assertThat(result.getAdvisorId()).isNull();
        assertThat(studentCaptor.getValue().getAdvisorId()).isNull();
        verify(studentProgramRepository).save(any(StudentProgram.class));
    }

    @Test
    @DisplayName("allows registration without secondLastName")
    void withoutSecondLastName() {
        RegisterStudentCommand command = sampleCommandBuilder()
                .advisorId(null)
                .secondLastName(null)
                .build();
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
                new PersonalData("Paulina", "Valencia", null, "Mexicana", LocalDate.of(1998, 3, 15), "5554821234", null),
                sampleAcademicInformation());
        ReflectionTestUtils.setField(savedStudent, "id", 1L);
        when(studentRepository.save(studentCaptor.capture())).thenReturn(savedStudent);
        when(studentProgramRepository.save(any(StudentProgram.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterStudentUseCase.RegisterStudentResult result = useCase.execute(command);

        assertThat(result.getSecondLastName()).isNull();
        assertThat(studentCaptor.getValue().getPersonalData().getSecondLastName()).isNull();
    }

    @Test
    @DisplayName("allows registration without phoneExtension")
    void withoutPhoneExtension() {
        RegisterStudentCommand command = sampleCommandBuilder().advisorId(null).phoneExtension(null).build();
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
                "2123803361", 20L, 1L, null, studentPersonalDataWithoutExtension(), sampleAcademicInformation());
        ReflectionTestUtils.setField(savedStudent, "id", 1L);
        when(studentRepository.save(studentCaptor.capture())).thenReturn(savedStudent);
        when(studentProgramRepository.save(any(StudentProgram.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterStudentUseCase.RegisterStudentResult result = useCase.execute(command);

        assertThat(result.getPhoneExtension()).isNull();
        assertThat(studentCaptor.getValue().getPersonalData().getPhoneExtension()).isNull();
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

    private static RegisterStudentCommand sampleCommand() {
        return sampleCommandBuilder().build();
    }

    private static RegisterStudentCommandBuilder sampleCommandBuilder() {
        return new RegisterStudentCommandBuilder();
    }

    private static final class RegisterStudentCommandBuilder {
        private Long graduateProgramId = 1L;
        private Long advisorId = 10L;
        private String enrollmentId = "2123803361";
        private String email = "paulina.valencia@uam.mx";
        private String firstName = "Paulina";
        private String firstLastName = "Valencia";
        private String secondLastName = "Franco";
        private String nationality = "Mexicana";
        private LocalDate birthDate = LocalDate.of(1998, 3, 15);
        private String phone = "5554821234";
        private String phoneExtension = "1234";
        private String undergraduateDegree = "Computación";
        private DegreeLevel lastDegreeObtained = DegreeLevel.LICENCIATURA;
        private ProgramType programType = ProgramType.MAESTRIA;
        private LocalDate admissionDate = LocalDate.of(2023, 9, 1);
        private String admissionTerm = "23O";

        RegisterStudentCommandBuilder graduateProgramId(Long value) {
            this.graduateProgramId = value;
            return this;
        }

        RegisterStudentCommandBuilder advisorId(Long value) {
            this.advisorId = value;
            return this;
        }

        RegisterStudentCommandBuilder secondLastName(String value) {
            this.secondLastName = value;
            return this;
        }

        RegisterStudentCommandBuilder phoneExtension(String value) {
            this.phoneExtension = value;
            return this;
        }

        RegisterStudentCommand build() {
            return new RegisterStudentCommand(
                    enrollmentId,
                    email,
                    graduateProgramId,
                    advisorId,
                    null,
                    null,
                    null,
                    null,
                    firstName,
                    firstLastName,
                    secondLastName,
                    nationality,
                    birthDate,
                    phone,
                    phoneExtension,
                    undergraduateDegree,
                    lastDegreeObtained,
                    programType,
                    admissionDate,
                    admissionTerm);
        }
    }
}

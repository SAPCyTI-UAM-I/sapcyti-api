package mx.uam.sapcyti.academic.application.service;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.defaultProfessorInformation;
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
import mx.uam.sapcyti.academic.application.command.RegisterProfessorCommand;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEmployeeNumberException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateProfessorEmailException;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorInformation;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
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
class RegisterProfessorUseCaseTest {

    @Mock private GraduateProgramRepositoryPort programRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private ProfessorRepositoryPort professorRepository;
    @Mock private PasswordGenerationService passwordGenerationService;
    @Mock private PasswordEncoderPort passwordEncoder;

    private RegisterProfessorUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterProfessorUseCase(
                programRepository,
                userRepository,
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
    @DisplayName("registers professor and user atomically")
    void happyPath() {
        RegisterProfessorCommand command = sampleCommand();
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(userRepository.existsByEmail("humberto.cervantes@uam.mx")).thenReturn(false);
        when(professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(1L, "30568"))
                .thenReturn(List.of());
        when(passwordGenerationService.generatePassword()).thenReturn("Rx7!nK4pWq2@");
        when(passwordEncoder.encode("Rx7!nK4pWq2@")).thenReturn("hashed");

        User savedUser = new User("humberto.cervantes@uam.mx", "hashed", RoleType.PROFESSOR, 1L);
        ReflectionTestUtils.setField(savedUser, "id", 20L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Professor savedProfessor = internoProfessor(
                "30568",
                20L,
                1L,
                professorPersonalData(),
                new ProfessorInformation(true, LocalDate.of(2027, 1, 15), LocalDate.of(2027, 7, 15)));
        ReflectionTestUtils.setField(savedProfessor, "id", 10L);
        when(professorRepository.save(any(Professor.class))).thenReturn(savedProfessor);

        RegisterProfessorUseCase.RegisterProfessorResult result = useCase.execute(command);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getProfessorType()).isEqualTo(ProfessorType.INTERNO);
        assertThat(result.getGeneratedPassword()).isEqualTo("Rx7!nK4pWq2@");
        assertThat(result.getEmail()).isEqualTo("humberto.cervantes@uam.mx");
        assertThat(result.isCommissionMember()).isTrue();
        assertThat(result.getNextSabbaticalStart()).isEqualTo(LocalDate.of(2027, 1, 15));
        verify(userRepository).save(any(User.class));
        verify(professorRepository).save(any(Professor.class));
    }

    @Test
    @DisplayName("rejects duplicate email")
    void duplicateEmail() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(userRepository.existsByEmail("humberto.cervantes@uam.mx")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(DuplicateProfessorEmailException.class);
    }

    @Test
    @DisplayName("rejects duplicate employee number")
    void duplicateEmployeeNumber() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(userRepository.existsByEmail("humberto.cervantes@uam.mx")).thenReturn(false);
        Professor existing = internoProfessor("30568", 99L, 1L, professorPersonalData());
        ReflectionTestUtils.setField(existing, "id", 5L);
        when(professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(1L, "30568"))
                .thenReturn(List.of(existing));
        when(userRepository.findById(99L)).thenReturn(Optional.of(
                new User("existing@uam.mx", "hash", RoleType.PROFESSOR, 1L)));

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(DuplicateEmployeeNumberException.class);
    }

    @Test
    @DisplayName("rejects unknown graduate program")
    void unknownProgram() {
        when(programRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(sampleCommand()))
                .isInstanceOf(GraduateProgramNotFoundException.class);
    }

    @Test
    @DisplayName("rejects tenant mismatch")
    void tenantMismatch() {
        RegisterProfessorCommand command = new RegisterProfessorCommand(
                ProfessorType.INTERNO,
                "30568",
                "humberto.cervantes@uam.mx",
                99L,
                "Humberto Gustavo",
                "Cervantes",
                "Maceda",
                "5554825678",
                "4321",
                true,
                LocalDate.of(2027, 1, 15),
                LocalDate.of(2027, 7, 15));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    @DisplayName("rejects invalid sabbatical period")
    void invalidSabbaticalPeriod() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));

        RegisterProfessorCommand command = new RegisterProfessorCommand(
                ProfessorType.INTERNO,
                "30568",
                "humberto.cervantes@uam.mx",
                1L,
                "Humberto Gustavo",
                "Cervantes",
                "Maceda",
                "5554825678",
                null,
                false,
                LocalDate.of(2027, 7, 15),
                LocalDate.of(2027, 1, 15));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sabbatical end date");
    }

    @Test
    @DisplayName("rejects interno without employee number")
    void internoWithoutEmployeeNumber() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));

        RegisterProfessorCommand command = new RegisterProfessorCommand(
                ProfessorType.INTERNO,
                null,
                "humberto.cervantes@uam.mx",
                1L,
                "Humberto Gustavo",
                "Cervantes",
                "Maceda",
                "5554825678",
                null,
                false,
                null,
                null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Employee number is required for internal professors");
    }

    @Test
    @DisplayName("rejects externo with employee number")
    void externoWithEmployeeNumber() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));

        RegisterProfessorCommand command = new RegisterProfessorCommand(
                ProfessorType.EXTERNO,
                "30568",
                "externo@uam.mx",
                1L,
                "Juan",
                "Perez",
                null,
                "5554825678",
                null,
                false,
                null,
                null);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Employee number must not be provided for external professors");
    }

    @Test
    @DisplayName("allows registration without secondLastName")
    void withoutSecondLastName() {
        RegisterProfessorCommand command = new RegisterProfessorCommand(
                ProfessorType.INTERNO,
                "30568",
                "humberto.cervantes@uam.mx",
                1L,
                "Humberto Gustavo",
                "Cervantes",
                null,
                "5554825678",
                null,
                false,
                null,
                null);
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(userRepository.existsByEmail("humberto.cervantes@uam.mx")).thenReturn(false);
        when(professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(1L, "30568"))
                .thenReturn(List.of());
        when(passwordGenerationService.generatePassword()).thenReturn("Rx7!nK4pWq2@");
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        User savedUser = new User("humberto.cervantes@uam.mx", "hashed", RoleType.PROFESSOR, 1L);
        ReflectionTestUtils.setField(savedUser, "id", 20L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ArgumentCaptor<Professor> professorCaptor = ArgumentCaptor.forClass(Professor.class);
        Professor savedProfessor = internoProfessor(
                "30568",
                20L,
                1L,
                new mx.uam.sapcyti.academic.domain.model.PersonalData(
                        "Humberto Gustavo", "Cervantes", null, null, null, "5554825678", null),
                defaultProfessorInformation());
        ReflectionTestUtils.setField(savedProfessor, "id", 10L);
        when(professorRepository.save(professorCaptor.capture())).thenReturn(savedProfessor);

        RegisterProfessorUseCase.RegisterProfessorResult result = useCase.execute(command);

        assertThat(result.getSecondLastName()).isNull();
        assertThat(professorCaptor.getValue().getPersonalData().getSecondLastName()).isNull();
    }

    @Test
    @DisplayName("allows registration without sabbatical dates")
    void withoutSabbaticalDates() {
        RegisterProfessorCommand command = new RegisterProfessorCommand(
                ProfessorType.INTERNO,
                "30568",
                "humberto.cervantes@uam.mx",
                1L,
                "Humberto Gustavo",
                "Cervantes",
                "Maceda",
                "5554825678",
                null,
                false,
                null,
                null);
        when(programRepository.findById(1L)).thenReturn(Optional.of(new GraduateProgram("PCyTI", "CBI")));
        when(userRepository.existsByEmail("humberto.cervantes@uam.mx")).thenReturn(false);
        when(professorRepository.findInternosByEmployeeNumberAndGraduateProgramId(1L, "30568"))
                .thenReturn(List.of());
        when(passwordGenerationService.generatePassword()).thenReturn("Rx7!nK4pWq2@");
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        User savedUser = new User("humberto.cervantes@uam.mx", "hashed", RoleType.PROFESSOR, 1L);
        ReflectionTestUtils.setField(savedUser, "id", 20L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ArgumentCaptor<Professor> professorCaptor = ArgumentCaptor.forClass(Professor.class);
        Professor savedProfessor = internoProfessor(
                "30568", 20L, 1L, professorPersonalData(), defaultProfessorInformation());
        ReflectionTestUtils.setField(savedProfessor, "id", 10L);
        when(professorRepository.save(professorCaptor.capture())).thenReturn(savedProfessor);

        RegisterProfessorUseCase.RegisterProfessorResult result = useCase.execute(command);

        assertThat(result.getNextSabbaticalStart()).isNull();
        assertThat(result.getNextSabbaticalEnd()).isNull();
        assertThat(professorCaptor.getValue().getProfessorInformation().getNextSabbaticalStart()).isNull();
    }

    private static RegisterProfessorCommand sampleCommand() {
        return new RegisterProfessorCommand(
                ProfessorType.INTERNO,
                "30568",
                "humberto.cervantes@uam.mx",
                1L,
                "Humberto Gustavo",
                "Cervantes",
                "Maceda",
                "5554825678",
                "4321",
                true,
                LocalDate.of(2027, 1, 15),
                LocalDate.of(2027, 7, 15));
    }
}

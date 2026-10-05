package mx.uam.sapcyti.academic.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.EnrollmentHistoryEntry;
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
class GetEnrollmentHistoryUseCaseTest {

    private static final Long PROGRAM_ID = 1L;
    private static final Long STUDENT_ID = 50L;
    private static final Long USER_ID = 500L;

    @Mock private StudentRepositoryPort studentRepository;
    @Mock private EnrollmentHistoryPort enrollmentHistoryPort;

    @InjectMocks
    private GetEnrollmentHistoryUseCase useCase;

    @BeforeEach
    void setUp() {
        TenantContext.set(PROGRAM_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("execute returns enrollment history when student belongs to current tenant")
    void executeSuccess() {
        Student student = sampleStudent();
        List<EnrollmentHistoryEntry> expectedEntries = List.of(
                new EnrollmentHistoryEntry(
                        "24-I", "24-I", "REGULAR", EnrollmentHistoryPort.HistoryPlanStatus.TERMINADA, null, List.of())
        );

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentHistoryPort.findByStudent(STUDENT_ID, PROGRAM_ID)).thenReturn(expectedEntries);

        List<EnrollmentHistoryEntry> actual = useCase.execute(STUDENT_ID);

        assertThat(actual).isEqualTo(expectedEntries);
        verify(enrollmentHistoryPort).findByStudent(STUDENT_ID, PROGRAM_ID);
    }

    @Test
    @DisplayName("execute throws StudentNotFoundException when student does not exist")
    void executeNotFound() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(STUDENT_ID))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    @DisplayName("execute throws StudentNotFoundException when student belongs to another tenant")
    void executeTenantMismatch() {
        Student otherTenantStudent = new Student(
                "2123803361", USER_ID, 999L, null,
                new PersonalData("Paulina", "Valencia", "Franco", "Mexicana", LocalDate.of(1998, 3, 15), "5554821234", null),
                new mx.uam.sapcyti.academic.domain.model.AcademicInformation(
                        "Computación", DegreeLevel.LICENCIATURA, ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), "23O"));

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(otherTenantStudent));

        assertThatThrownBy(() -> useCase.execute(STUDENT_ID))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    @DisplayName("executeByUserId returns enrollment history when student is found for user and tenant")
    void executeByUserIdSuccess() {
        Student student = sampleStudent();
        List<EnrollmentHistoryEntry> expectedEntries = List.of(
                new EnrollmentHistoryEntry(
                        "24-I", "24-I", "REGULAR", EnrollmentHistoryPort.HistoryPlanStatus.TERMINADA, null, List.of())
        );

        when(studentRepository.findByUserIdAndGraduateProgramId(USER_ID, PROGRAM_ID))
                .thenReturn(Optional.of(student));
        when(enrollmentHistoryPort.findByStudent(STUDENT_ID, PROGRAM_ID)).thenReturn(expectedEntries);

        List<EnrollmentHistoryEntry> actual = useCase.executeByUserId(USER_ID);

        assertThat(actual).isEqualTo(expectedEntries);
    }

    @Test
    @DisplayName("executeByUserId throws StudentNotFoundException when student is not found")
    void executeByUserIdNotFound() {
        when(studentRepository.findByUserIdAndGraduateProgramId(USER_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executeByUserId(USER_ID))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    @DisplayName("execute throws TenantAccessDeniedException when tenant context is missing")
    void executeMissingTenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> useCase.execute(STUDENT_ID))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    private static Student sampleStudent() {
        Student student = new Student(
                "2123803361", USER_ID, PROGRAM_ID, null,
                new PersonalData("Paulina", "Valencia", "Franco", "Mexicana", LocalDate.of(1998, 3, 15), "5554821234", null),
                new mx.uam.sapcyti.academic.domain.model.AcademicInformation(
                        "Computación", DegreeLevel.LICENCIATURA, ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1), "23O"));
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);
        return student;
    }
}

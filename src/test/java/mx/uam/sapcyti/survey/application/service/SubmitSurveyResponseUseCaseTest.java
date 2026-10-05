package mx.uam.sapcyti.survey.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.AcademicInformation;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.application.command.SubmitResponseCommand;
import mx.uam.sapcyti.survey.domain.exception.BlankWithUeasConflictException;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotActiveException;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmitSurveyResponseUseCaseTest {

    @Mock
    private SurveyResponseRepositoryPort responseRepository;

    @Mock
    private UeaRepositoryPort ueaRepository;

    @Mock
    private SurveyDetailFactory surveyDetailFactory;

    @Mock
    private StudentSurveyContextResolver studentContextResolver;

    @InjectMocks
    private SubmitSurveyResponseUseCase useCase;

    @BeforeEach
    void setUp() {
        mx.uam.sapcyti.shared.tenant.TenantContext.set(1L);
    }

    @AfterEach
    void tearDown() {
        mx.uam.sapcyti.shared.tenant.TenantContext.clear();
    }

    @Test
    @DisplayName("rejects BLANK mode with ueaIds")
    void blankWithUeas() {
        EnrollmentSurvey survey = activeSurvey();
        when(surveyDetailFactory.requireSurvey(1L)).thenReturn(survey);

        assertThatThrownBy(() -> useCase.execute(
                        1L,
                        new SubmitResponseCommand(
                                AcademicTerm.I, SurveyResponseMode.BLANK, List.of(5L))))
                .isInstanceOf(BlankWithUeasConflictException.class);
    }

    @Test
    @DisplayName("rejects submit when survey is not ACTIVO")
    void notActive() {
        EnrollmentSurvey survey = EnrollmentSurvey.create(
                1L,
                "26O",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"),
                null,
                1L,
                List.of());
        when(surveyDetailFactory.requireSurvey(1L)).thenReturn(survey);

        assertThatThrownBy(() -> useCase.execute(
                        1L,
                        new SubmitResponseCommand(
                                AcademicTerm.I, SurveyResponseMode.BLANK, List.of())))
                .isInstanceOf(SurveyNotActiveException.class);
    }

    @Test
    @DisplayName("upserts ENROLL_UEAS response and computes totalUeas")
    void upsertEnrollUeas() {
        EnrollmentSurvey survey = activeSurvey();
        UEA uea = org.mockito.Mockito.mock(UEA.class);
        when(uea.getId()).thenReturn(5L);
        when(surveyDetailFactory.requireSurvey(1L)).thenReturn(survey);
        when(ueaRepository.findActiveByGraduateProgramId(1L)).thenReturn(List.of(uea));
        when(studentContextResolver.requireCurrentStudent()).thenReturn(sampleStudent(99L));
        when(responseRepository.findBySurveyIdAndStudentId(1L, 99L)).thenReturn(Optional.empty());
        when(responseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SubmittedResponseView result = useCase.execute(
                1L,
                new SubmitResponseCommand(AcademicTerm.III, SurveyResponseMode.ENROLL_UEAS, List.of(5L)));

        assertThat(result.getTotalUeas()).isEqualTo(1);
        assertThat(result.getMode()).isEqualTo(SurveyResponseMode.ENROLL_UEAS);

        ArgumentCaptor<StudentSurveyResponse> captor = ArgumentCaptor.forClass(StudentSurveyResponse.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getUeaIds()).containsExactly(5L);
    }

    private static EnrollmentSurvey activeSurvey() {
        return EnrollmentSurvey.create(
                1L,
                "26O",
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600),
                null,
                1L,
                List.of(5L));
    }

    private static Student sampleStudent(Long id) {
        Student student = new Student(
                "2123000001",
                2L,
                1L,
                null,
                new PersonalData("Ana", "Lopez", "Perez", "MX", null, "555", null),
                new AcademicInformation("CS", DegreeLevel.LICENCIATURA, ProgramType.MAESTRIA, null, "23O"));
        try {
            var field = Student.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(student, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return student;
    }
}

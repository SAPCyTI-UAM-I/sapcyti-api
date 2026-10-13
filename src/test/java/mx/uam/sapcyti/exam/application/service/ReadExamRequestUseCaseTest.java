package mx.uam.sapcyti.exam.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.academic.domain.model.StudentExamContextOutcome;
import mx.uam.sapcyti.academic.domain.model.StudentExamContextResult;
import mx.uam.sapcyti.academic.domain.port.out.StudentExamContextPort;
import mx.uam.sapcyti.exam.application.exception.ExamReadException;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamRequestBinding;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import mx.uam.sapcyti.exam.domain.service.DegreeExamCatalog;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataAccessResourceFailureException;

class ReadExamRequestUseCaseTest {

    private static final Long USER_ID = 1L;
    private static final Long PROGRAM_ID = 17L;

    private StudentExamContextPort contexts;
    private ExamRequestReadPort dossiers;
    private DegreeExamCatalog catalog;
    private ReadExamRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        contexts = mock(StudentExamContextPort.class);
        dossiers = mock(ExamRequestReadPort.class);
        catalog = spy(new DegreeExamCatalog(dossiers));
        useCase = new ReadExamRequestUseCase(contexts, catalog);
        lenient().when(dossiers.findByStudentAndType(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("A caller who is not a student is denied before the academic read")
    void nonStudentDoesNotReadTheAcademicPort() {
        assertThatThrownBy(() -> useCase.options(USER_ID, RoleType.COORDINATOR, PROGRAM_ID))
                .isInstanceOf(ExamReadException.class)
                .extracting(error -> ((ExamReadException) error).code())
                .isEqualTo(ExamReadException.Code.FORBIDDEN);
        verifyNoInteractions(contexts);
        verify(catalog, never()).options(any());
    }

    @Test
    @DisplayName("Doctoral options keep PREDOCTORAL after GRADO")
    void doctoralOptionsIncludePredoctoral() {
        when(contexts.resolve(USER_ID, PROGRAM_ID))
                .thenReturn(StudentExamContextResult.resolved(context(ProgramType.DOCTORADO)));

        var options = useCase.options(USER_ID, RoleType.STUDENT, PROGRAM_ID);

        assertThat(options.examTypes()).containsExactly(ExamType.GRADO, ExamType.PREDOCTORAL);
        verify(dossiers, never()).findByStudentAndType(any(), any());
    }

    @Test
    @DisplayName("Specialization or master's cannot request the predoctoral exam")
    void predoctoralIsRefusedForMaster() {
        when(contexts.resolve(USER_ID, PROGRAM_ID))
                .thenReturn(StudentExamContextResult.resolved(context(ProgramType.MAESTRIA)));

        assertThatThrownBy(() -> useCase.requirements(USER_ID, RoleType.STUDENT, PROGRAM_ID, ExamType.PREDOCTORAL))
                .isInstanceOf(ExamReadException.class)
                .extracting(error -> ((ExamReadException) error).code())
                .isEqualTo(ExamReadException.Code.EXAM_TYPE_NOT_ALLOWED);
        verify(catalog, never()).requirements(any());
    }

    @Test
    @DisplayName("A doctorate PREDOCTORAL read calls the catalog with that type")
    void doctoralPredoctoralUsesThatCatalog() {
        when(contexts.resolve(USER_ID, PROGRAM_ID))
                .thenReturn(StudentExamContextResult.resolved(context(ProgramType.DOCTORADO)));

        var requirements = useCase.requirements(USER_ID, RoleType.STUDENT, PROGRAM_ID, ExamType.PREDOCTORAL);

        assertThat(requirements.examType()).isEqualTo(ExamType.PREDOCTORAL);
        assertThat(requirements.request()).isNull();
        assertThat(requirements.requirements()).extracting(item -> item.documentType()).containsExactly(
                DocumentType.TESIS,
                DocumentType.ARTICULO,
                DocumentType.SOL_SINODALES,
                DocumentType.HIST_ACAD,
                DocumentType.COMP_IDIOMA,
                DocumentType.DATOS_ALUM);
        assertThat(requirements.requirements())
                .filteredOn(item -> item.documentType() == DocumentType.ARTICULO)
                .singleElement()
                .extracting(item -> item.required())
                .isEqualTo(false);
        verify(catalog).requirements(any(), eq(ExamType.PREDOCTORAL));
        verify(catalog, never()).requirements(any());
        verify(dossiers).findByStudentAndType(52L, ExamType.PREDOCTORAL);
        verify(dossiers, never()).findByStudentAndType(any(), eq(ExamType.GRADO));
    }

    @Test
    @DisplayName("A request tied to another program is a conflict and does not invent a catalog")
    void mismatchedBindingIsAConflict() {
        when(contexts.resolve(USER_ID, PROGRAM_ID))
                .thenReturn(StudentExamContextResult.resolved(context(ProgramType.MAESTRIA)));
        when(dossiers.findByStudentAndType(52L, ExamType.GRADO)).thenReturn(Optional.of(new ExamRequestRead(
                new ExamRequestBinding(
                        8L, 52L, ExamType.GRADO, 99L, Degree.MAESTRIA, ExamRequestStatus.BORRADOR, null, 1, null),
                List.of())));

        assertThatThrownBy(() -> useCase.requirements(USER_ID, RoleType.STUDENT, PROGRAM_ID, ExamType.GRADO))
                .isInstanceOf(ExamReadException.class)
                .extracting(error -> ((ExamReadException) error).code())
                .isEqualTo(ExamReadException.Code.ACADEMIC_PROGRAM_CHANGED);
    }

    @ParameterizedTest
    @EnumSource(value = StudentExamContextOutcome.class, names = {
            "NOT_FOUND", "AMBIGUOUS", "INCOMPLETE", "DEGREE_UNSUPPORTED", "STATUS_UNSUPPORTED"
    })
    @DisplayName("An unresolved academic outcome does not build a catalog")
    void unresolvedOutcomeDoesNotBuildACatalog(StudentExamContextOutcome outcome) {
        when(contexts.resolve(USER_ID, PROGRAM_ID)).thenReturn(StudentExamContextResult.of(outcome));

        assertThatThrownBy(() -> useCase.options(USER_ID, RoleType.STUDENT, PROGRAM_ID))
                .isInstanceOf(ExamReadException.class)
                .extracting(error -> ((ExamReadException) error).code())
                .isEqualTo(expected(outcome));
        verify(catalog, never()).options(any());
    }

    @Test
    @DisplayName("A transient academic failure does not invent a catalog")
    void academicFailureIsUnavailable() {
        when(contexts.resolve(USER_ID, PROGRAM_ID))
                .thenThrow(new DataAccessResourceFailureException("down"));

        assertThatThrownBy(() -> useCase.options(USER_ID, RoleType.STUDENT, PROGRAM_ID))
                .isInstanceOf(ExamReadException.class)
                .extracting(error -> ((ExamReadException) error).code())
                .isEqualTo(ExamReadException.Code.DEPENDENCY_UNAVAILABLE);
        verify(catalog, never()).options(any());
    }

    @Test
    @DisplayName("A transient dossier failure does not invent a catalog")
    void dossierFailureIsUnavailable() {
        when(contexts.resolve(USER_ID, PROGRAM_ID))
                .thenReturn(StudentExamContextResult.resolved(context(ProgramType.MAESTRIA)));
        when(dossiers.findByStudentAndType(52L, ExamType.GRADO))
                .thenThrow(new DataAccessResourceFailureException("down"));

        assertThatThrownBy(() -> useCase.requirements(USER_ID, RoleType.STUDENT, PROGRAM_ID, ExamType.GRADO))
                .isInstanceOf(ExamReadException.class)
                .extracting(error -> ((ExamReadException) error).code())
                .isEqualTo(ExamReadException.Code.DEPENDENCY_UNAVAILABLE);
    }

    private static ExamReadException.Code expected(StudentExamContextOutcome outcome) {
        return switch (outcome) {
            case NOT_FOUND -> ExamReadException.Code.STUDENT_NOT_FOUND;
            case AMBIGUOUS -> ExamReadException.Code.ACADEMIC_PROGRAM_AMBIGUOUS;
            case INCOMPLETE -> ExamReadException.Code.STUDENT_DATA_INCOMPLETE;
            case DEGREE_UNSUPPORTED -> ExamReadException.Code.DEGREE_UNSUPPORTED;
            case STATUS_UNSUPPORTED -> ExamReadException.Code.PROGRAM_STATUS_UNSUPPORTED;
            case RESOLVED -> throw new IllegalArgumentException("resolved is not an error");
        };
    }

    private static StudentExamContext context(ProgramType programType) {
        return new StudentExamContext(
                52L,
                PROGRAM_ID,
                102L,
                programType,
                ProgramStatus.EN_INVESTIGACION,
                "Emanuel",
                "López",
                "Alba");
    }
}

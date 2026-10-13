package mx.uam.sapcyti.exam.domain.service;

import static mx.uam.sapcyti.academic.domain.model.ProgramStatus.BAJA;
import static mx.uam.sapcyti.academic.domain.model.ProgramStatus.EGRESADO;
import static mx.uam.sapcyti.academic.domain.model.ProgramStatus.SUSPENSION;
import static mx.uam.sapcyti.academic.domain.model.ProgramType.DOCTORADO;
import static mx.uam.sapcyti.academic.domain.model.ProgramType.ESPECIALIZACION;
import static mx.uam.sapcyti.academic.domain.model.ProgramType.MAESTRIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamRequestBinding;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.SubmitCompletenessResult;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamSubmitCompletenessTest {

    private static final Long STUDENT_ID = 10L;
    private static final Long PROGRAM_ID = 17L;
    private static final Instant UPLOADED_AT = Instant.parse("2026-03-01T15:04:05Z");

    @Mock
    private ExamRequestReadPort readPort;

    private ExamSubmitCompleteness completeness;

    @BeforeEach
    void setUp() {
        completeness = new ExamSubmitCompleteness(new DegreeExamCatalog(readPort));
    }

    @Test
    @DisplayName("A predoctoral request can be submitted without the article")
    void predoctoralCanBeSubmittedWithoutTheArticle() {
        ExamRequestBinding binding = draft(ExamType.PREDOCTORAL, Degree.DOCTORADO);

        SubmitCompletenessResult result = completeness.evaluate(
                ana(), ExamType.PREDOCTORAL, new ExamRequestRead(binding, loadedWithoutArticle()));

        assertThat(result.error()).isNull();
        assertThat(result.completeness().examType()).isEqualTo(ExamType.PREDOCTORAL);
        assertThat(result.completeness().degree()).isEqualTo(Degree.DOCTORADO);
        assertThat(result.completeness().missingDocumentTypes()).isEmpty();
        assertThat(binding.status()).isEqualTo(ExamRequestStatus.BORRADOR);
        assertThat(binding.folio()).isNull();
        verifyNoInteractions(readPort);
    }

    @Test
    @DisplayName("A doctoral degree exam cannot be submitted without the article")
    void doctoralDegreeExamCannotBeSubmittedWithoutTheArticle() {
        ExamRequestBinding binding = draft(ExamType.GRADO, Degree.DOCTORADO);

        SubmitCompletenessResult result = completeness.evaluate(
                ana(), ExamType.GRADO, new ExamRequestRead(binding, loadedWithoutArticle()));

        assertThat(result.error()).isEqualTo(SubmitCompletenessResult.REQUIRED_DOCUMENTS_MISSING);
        assertThat(result.completeness().missingDocumentTypes()).containsExactly(DocumentType.ARTICULO);
        assertThat(binding.status()).isEqualTo(ExamRequestStatus.BORRADOR);
        assertThat(binding.folio()).isNull();
        verifyNoInteractions(readPort);
    }

    @ParameterizedTest(name = "{0} cannot create a predoctoral request")
    @MethodSource("nonDoctoralDegrees")
    @DisplayName("A non-doctoral student cannot create a predoctoral request")
    void nonDoctoralStudentCannotCreateAPredoctoralRequest(ProgramType programType) {
        StudentExamContext context = context(programType, EGRESADO);

        SubmitCompletenessResult result = completeness.evaluate(context, ExamType.PREDOCTORAL, null);

        assertThat(result.error()).isEqualTo(SubmitCompletenessResult.EXAM_TYPE_NOT_ALLOWED);
        assertThat(result.completeness()).isNull();
        verifyNoInteractions(readPort);
    }

    @ParameterizedTest(name = "{0} does not build missing document types")
    @MethodSource("ineligibleStatuses")
    @DisplayName("Baja or suspension can be read and cannot be written")
    void bajaOrSuspensionDoesNotBuildMissingDocumentTypes(ProgramStatus status) {
        ExamRequestBinding binding = draft(ExamType.GRADO, Degree.DOCTORADO);

        SubmitCompletenessResult result = completeness.evaluate(
                context(DOCTORADO, status),
                ExamType.GRADO,
                new ExamRequestRead(binding, loadedWithoutArticle()));

        assertThat(result.error()).isEqualTo(DegreeExamCatalog.PROGRAM_NOT_ELIGIBLE);
        assertThat(result.completeness()).isNull();
        assertThat(binding.status()).isEqualTo(ExamRequestStatus.BORRADOR);
        assertThat(binding.folio()).isNull();
        verifyNoInteractions(readPort);
    }

    private static Stream<ProgramType> nonDoctoralDegrees() {
        return Stream.of(MAESTRIA, ESPECIALIZACION);
    }

    private static Stream<ProgramStatus> ineligibleStatuses() {
        return Stream.of(BAJA, SUSPENSION);
    }

    private static StudentExamContext ana() {
        return context(DOCTORADO, EGRESADO);
    }

    private static StudentExamContext context(ProgramType programType, ProgramStatus programStatus) {
        return new StudentExamContext(
                STUDENT_ID, PROGRAM_ID, 100L, programType, programStatus, "Ana", "López", "Alba");
    }

    private static ExamRequestBinding draft(ExamType examType, Degree degree) {
        return new ExamRequestBinding(
                3L, STUDENT_ID, examType, PROGRAM_ID, degree, ExamRequestStatus.BORRADOR, null, 1, null);
    }

    private static List<CurrentExamDocument> loadedWithoutArticle() {
        return List.of(
                pdf(1L, DocumentType.TESIS),
                pdf(2L, DocumentType.SOL_SINODALES),
                pdf(3L, DocumentType.HIST_ACAD),
                pdf(4L, DocumentType.COMP_IDIOMA),
                pdf(5L, DocumentType.DATOS_ALUM));
    }

    private static CurrentExamDocument pdf(Long id, DocumentType documentType) {
        return new CurrentExamDocument(id, documentType, DocumentKind.PDF, documentType.name() + ".pdf", null, 100L, UPLOADED_AT);
    }
}

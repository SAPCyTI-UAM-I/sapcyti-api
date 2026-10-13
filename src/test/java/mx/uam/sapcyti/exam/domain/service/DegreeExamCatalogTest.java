package mx.uam.sapcyti.exam.domain.service;

import static mx.uam.sapcyti.academic.domain.model.ProgramStatus.BAJA;
import static mx.uam.sapcyti.academic.domain.model.ProgramStatus.EGRESADO;
import static mx.uam.sapcyti.academic.domain.model.ProgramStatus.EN_INVESTIGACION;
import static mx.uam.sapcyti.academic.domain.model.ProgramStatus.SUSPENSION;
import static mx.uam.sapcyti.academic.domain.model.ProgramType.DOCTORADO;
import static mx.uam.sapcyti.academic.domain.model.ProgramType.ESPECIALIZACION;
import static mx.uam.sapcyti.academic.domain.model.ProgramType.MAESTRIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.DegreeExamRequirementsResult;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.EligibilityReason;
import mx.uam.sapcyti.exam.domain.model.ExamOptions;
import mx.uam.sapcyti.exam.domain.model.ExamRequestBinding;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamRequirement;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.RequirementStatus;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DegreeExamCatalogTest {

    private static final Long STUDENT_ID = 10L;
    private static final Long PROGRAM_ID = 17L;
    private static final Instant UPLOADED_AT = Instant.parse("2026-03-01T15:04:05Z");

    @Mock
    private ExamRequestReadPort readPort;

    private DegreeExamCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new DegreeExamCatalog(readPort);
    }

    @Test
    @DisplayName("Specialization student sees five degree documents")
    void specializationSeesFiveDegreeDocuments() {
        StudentExamContext context = context(ESPECIALIZACION, EN_INVESTIGACION, "Ana", "Perez", "Lopez");
        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.GRADO)).thenReturn(Optional.empty());

        ExamOptions options = catalog.options(context);
        ExamRequirements requirements = ready(context);

        assertThat(options.degree()).isEqualTo(Degree.ESPECIALIZACION);
        assertThat(options.programStatus().name()).isEqualTo(EN_INVESTIGACION.name());
        assertThat(options.eligible()).isTrue();
        assertThat(options.eligibilityReason()).isNull();
        assertThat(options.examTypes()).containsExactly(ExamType.GRADO);
        assertThat(requirements.examType()).isEqualTo(ExamType.GRADO);
        assertThat(requirements.degree()).isEqualTo(Degree.ESPECIALIZACION);
        assertThat(requirements.request()).isNull();
        assertThat(requirements.requirements()).extracting(ExamRequirement::documentType).containsExactly(
                DocumentType.TESIS,
                DocumentType.SOL_SINODALES,
                DocumentType.HIST_ACAD,
                DocumentType.COMP_IDIOMA,
                DocumentType.DATOS_ALUM);
        assertThat(requirements.requirements()).extracting(ExamRequirement::documentType)
                .doesNotContain(DocumentType.ARTICULO);
        assertThat(requirements.requirements()).allSatisfy(item -> {
            assertThat(item.required()).isTrue();
            assertThat(item.acceptedKinds()).containsExactly(DocumentKind.PDF);
            assertThat(item.status()).isEqualTo(RequirementStatus.PENDIENTE);
            assertThat(item.currentDocument()).isNull();
        });
        ExamRequirement thesis = item(requirements, DocumentType.TESIS);
        assertThat(thesis.expectedName()).startsWith("Tesis_Esp_");
        assertThat(thesis.exampleName()).isEqualTo(thesis.expectedName());
        verify(readPort).findByStudentAndType(STUDENT_ID, ExamType.GRADO);
        verifyNoMoreInteractions(readPort);
    }

    @Test
    @DisplayName("Master's student sees five degree documents")
    void mastersSeesFiveDegreeDocuments() {
        StudentExamContext context = emanuel(MAESTRIA);
        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.GRADO)).thenReturn(Optional.empty());

        ExamRequirements requirements = ready(context);

        assertThat(requirements.requirements()).hasSize(5);
        assertThat(requirements.requirements()).extracting(ExamRequirement::documentType)
                .doesNotContain(DocumentType.ARTICULO);
        assertThat(item(requirements, DocumentType.TESIS).expectedName())
                .isEqualTo("Tesis_Mtria_Lopez_Alba_Emanuel.pdf");
        assertThat(item(requirements, DocumentType.TESIS).exampleName())
                .isEqualTo("Tesis_Mtria_Lopez_Alba_Emanuel.pdf");
        assertThat(item(requirements, DocumentType.SOL_SINODALES).exampleName())
                .isEqualTo("Sol_Sinodales_Lopez_Alba_Emanuel.pdf");
        assertThat(item(requirements, DocumentType.HIST_ACAD).exampleName())
                .isEqualTo("Hist_Acad_Lopez_Alba_Emanuel.pdf");
        assertThat(item(requirements, DocumentType.COMP_IDIOMA).exampleName())
                .isEqualTo("Comp_Idioma_Lopez_Alba_Emanuel.pdf");
        assertThat(item(requirements, DocumentType.DATOS_ALUM).exampleName())
                .isEqualTo("Datos_Alum_Lopez_Alba_Emanuel.pdf");
        assertThat(requirements.requirements()).allSatisfy(requirement ->
                assertThat(requirement.exampleName()).isEqualTo(requirement.expectedName()));
    }

    @Test
    @DisplayName("Doctoral student sees six degree documents including the article")
    void doctoralSeesSixDegreeDocumentsIncludingTheArticle() {
        StudentExamContext context = emanuel(DOCTORADO);
        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.GRADO)).thenReturn(Optional.empty());

        ExamOptions options = catalog.options(context);
        ExamRequirements requirements = ready(context);

        assertThat(options.examTypes()).containsExactly(ExamType.GRADO, ExamType.PREDOCTORAL);
        assertThat(requirements.requirements()).extracting(ExamRequirement::documentType).containsExactly(
                DocumentType.TESIS,
                DocumentType.ARTICULO,
                DocumentType.SOL_SINODALES,
                DocumentType.HIST_ACAD,
                DocumentType.COMP_IDIOMA,
                DocumentType.DATOS_ALUM);
        ExamRequirement article = item(requirements, DocumentType.ARTICULO);
        assertThat(article.required()).isTrue();
        assertThat(article.acceptedKinds()).containsExactly(DocumentKind.PDF, DocumentKind.URL);
        assertThat(article.expectedName()).isNull();
        assertThat(article.exampleName()).isNull();
        assertThat(item(requirements, DocumentType.TESIS).expectedName()).startsWith("Tesis_Docto_");
        assertThat(item(requirements, DocumentType.TESIS).exampleName())
                .isEqualTo(item(requirements, DocumentType.TESIS).expectedName());

        ExamRequirements byType = ready(context, ExamType.GRADO);
        assertThat(item(byType, DocumentType.ARTICULO).required()).isTrue();
    }

    @Test
    @DisplayName("Predoctoral documents match the doctoral list and the article is optional")
    void predoctoralDocumentsMatchTheDoctoralListAndTheArticleIsOptional() {
        StudentExamContext context = context(DOCTORADO, EGRESADO, "Ana", "López", "Alba");
        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.PREDOCTORAL)).thenReturn(Optional.empty());

        ExamRequirements requirements = ready(context, ExamType.PREDOCTORAL);

        assertThat(requirements.examType()).isEqualTo(ExamType.PREDOCTORAL);
        assertThat(requirements.degree()).isEqualTo(Degree.DOCTORADO);
        assertThat(requirements.request()).isNull();
        assertThat(requirements.requirements()).extracting(ExamRequirement::documentType).containsExactly(
                DocumentType.TESIS,
                DocumentType.ARTICULO,
                DocumentType.SOL_SINODALES,
                DocumentType.HIST_ACAD,
                DocumentType.COMP_IDIOMA,
                DocumentType.DATOS_ALUM);
        assertThat(requirements.requirements()).filteredOn(item -> item.documentType() != DocumentType.ARTICULO)
                .allSatisfy(item -> {
                    assertThat(item.required()).isTrue();
                    assertThat(item.acceptedKinds()).containsExactly(DocumentKind.PDF);
                    assertThat(item.status()).isEqualTo(RequirementStatus.PENDIENTE);
                    assertThat(item.currentDocument()).isNull();
                });
        ExamRequirement article = item(requirements, DocumentType.ARTICULO);
        assertThat(article.required()).isFalse();
        assertThat(article.acceptedKinds()).containsExactly(DocumentKind.PDF, DocumentKind.URL);
        assertThat(article.expectedName()).isNull();
        assertThat(article.exampleName()).isNull();
        assertThat(item(requirements, DocumentType.TESIS).expectedName())
                .isEqualTo("Tesis_Docto_Lopez_Alba_Ana.pdf");
        assertThat(item(requirements, DocumentType.TESIS).exampleName())
                .isEqualTo("Tesis_Docto_Lopez_Alba_Ana.pdf");
        assertThat(item(requirements, DocumentType.SOL_SINODALES).exampleName())
                .isEqualTo("Sol_Sinodales_Lopez_Alba_Ana.pdf");
        assertThat(item(requirements, DocumentType.HIST_ACAD).exampleName())
                .isEqualTo("Hist_Acad_Lopez_Alba_Ana.pdf");
        assertThat(item(requirements, DocumentType.COMP_IDIOMA).exampleName())
                .isEqualTo("Comp_Idioma_Lopez_Alba_Ana.pdf");
        assertThat(item(requirements, DocumentType.DATOS_ALUM).exampleName())
                .isEqualTo("Datos_Alum_Lopez_Alba_Ana.pdf");
        assertThat(requirements.requirements()).allSatisfy(requirement ->
                assertThat(requirement.exampleName()).isEqualTo(requirement.expectedName()));
        verify(readPort).findByStudentAndType(STUDENT_ID, ExamType.PREDOCTORAL);
        verifyNoMoreInteractions(readPort);
    }

    @ParameterizedTest(name = "{0} cannot read or open a predoctoral request")
    @MethodSource("nonDoctoralDegrees")
    @DisplayName("Specialization cannot read the predoctoral exam")
    void nonDoctoralPredoctoralIsExamTypeNotAllowed(ProgramType programType) {
        StudentExamContext context = context(programType, EGRESADO, "Ana", "López", "Alba");

        DegreeExamRequirementsResult result = catalog.requirements(context, ExamType.PREDOCTORAL);

        assertThat(result.error()).isEqualTo(DegreeExamRequirementsResult.EXAM_TYPE_NOT_ALLOWED);
        assertThat(result.requirements()).isNull();
        verifyNoInteractions(readPort);
    }

    @Test
    @DisplayName("The degree exam stays a separate request")
    void degreeExamStaysASeparateRequest() {
        StudentExamContext context = context(DOCTORADO, EGRESADO, "Ana", "López", "Alba");
        CurrentExamDocument thesis = new CurrentExamDocument(
                8L, DocumentType.TESIS, DocumentKind.PDF, "Tesis_Docto_Lopez_Alba_Ana.pdf", null, 2048L, UPLOADED_AT);
        ExamRequestBinding predoctoral = new ExamRequestBinding(
                4L,
                STUDENT_ID,
                ExamType.PREDOCTORAL,
                PROGRAM_ID,
                Degree.DOCTORADO,
                ExamRequestStatus.ENVIADA,
                "SEG-482913-075316",
                2,
                UPLOADED_AT);
        when(readPort.findByStudentAndType(eq(STUDENT_ID), any())).thenAnswer(invocation -> {
            ExamType asked = invocation.getArgument(1);
            if (asked == ExamType.PREDOCTORAL) {
                return Optional.of(new ExamRequestRead(predoctoral, List.of(thesis)));
            }
            return Optional.empty();
        });

        ExamRequirements grado = ready(context, ExamType.GRADO);

        assertThat(grado.request()).isNull();
        assertThat(grado.requirements()).allSatisfy(item -> {
            assertThat(item.status()).isEqualTo(RequirementStatus.PENDIENTE);
            assertThat(item.currentDocument()).isNull();
        });
        assertThat(item(grado, DocumentType.ARTICULO).required()).isTrue();
        assertThat(predoctoral.status()).isEqualTo(ExamRequestStatus.ENVIADA);
        assertThat(predoctoral.folio()).isEqualTo("SEG-482913-075316");
        assertThat(predoctoral.version()).isEqualTo(2);
        verify(readPort).findByStudentAndType(STUDENT_ID, ExamType.GRADO);
        verifyNoMoreInteractions(readPort);
    }

    @Test
    @DisplayName("Official names drop accents, ñ and internal spaces")
    void officialNamesDropAccentsEnieAndInternalSpaces() {
        StudentExamContext context = context(ESPECIALIZACION, EN_INVESTIGACION, "Ana María", "Muñoz", "Ibañez");
        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.GRADO)).thenReturn(Optional.empty());

        ExamRequirement thesis = item(ready(context), DocumentType.TESIS);

        assertThat(thesis.expectedName()).isEqualTo("Tesis_Esp_Munoz_Ibanez_Ana_Maria.pdf");
        assertThat(thesis.exampleName()).isEqualTo(thesis.expectedName());
    }

    @Test
    @DisplayName("A missing maternal surname is omitted from the file name")
    void missingMaternalSurnameIsOmitted() {
        StudentExamContext context = context(MAESTRIA, EN_INVESTIGACION, "Emanuel", "López", null);
        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.GRADO)).thenReturn(Optional.empty());

        String thesis = item(ready(context), DocumentType.TESIS).expectedName();

        assertThat(thesis).isEqualTo("Tesis_Mtria_Lopez_Emanuel.pdf");
        assertThat(thesis).doesNotContain("__");
    }

    @Test
    @DisplayName("An egresado student is eligible")
    void egresadoIsEligible() {
        StudentExamContext context = emanuel(MAESTRIA, EGRESADO);

        ExamOptions options = catalog.options(context);

        assertThat(options.programStatus().name()).isEqualTo(EGRESADO.name());
        assertThat(options.eligible()).isTrue();
        assertThat(options.eligibilityReason()).isNull();
        assertThat(catalog.writeRejection(context)).isEmpty();
        verifyNoInteractions(readPort);
    }

    @ParameterizedTest(name = "{0} reads as {1} and rejects writes")
    @MethodSource("ineligibleStatuses")
    @DisplayName("Baja or suspension can be read and cannot be written")
    void bajaOrSuspensionCanBeReadAndCannotBeWritten(ProgramStatus status, EligibilityReason reason) {
        StudentExamContext context = emanuel(MAESTRIA, status);
        assertThat(catalog.writeRejection(context)).contains(DegreeExamCatalog.PROGRAM_NOT_ELIGIBLE);
        verifyNoInteractions(readPort);

        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.GRADO)).thenReturn(Optional.empty());
        ExamRequirements requirements = ready(context);

        assertThat(requirements.programStatus().name()).isEqualTo(status.name());
        assertThat(requirements.eligible()).isFalse();
        assertThat(requirements.eligibilityReason()).isEqualTo(reason);
        assertThat(requirements.requirements()).hasSize(5);
        verify(readPort).findByStudentAndType(STUDENT_ID, ExamType.GRADO);
        verifyNoMoreInteractions(readPort);
    }

    @Test
    @DisplayName("A read without a request leaves every document pending")
    void readWithoutRequestLeavesEveryDocumentPending() {
        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.GRADO)).thenReturn(Optional.empty());

        ExamRequirements requirements = ready(emanuel(MAESTRIA));

        assertThat(requirements.request()).isNull();
        assertThat(requirements.requirements()).allSatisfy(requirement -> {
            assertThat(requirement.status()).isEqualTo(RequirementStatus.PENDIENTE);
            assertThat(requirement.currentDocument()).isNull();
        });
        verify(readPort).findByStudentAndType(STUDENT_ID, ExamType.GRADO);
        verifyNoMoreInteractions(readPort);
    }

    @Test
    @DisplayName("A current document is shown without its storage path")
    void currentDocumentIsShownWithoutStoragePath() {
        ExamRequestBinding binding = binding(PROGRAM_ID, Degree.MAESTRIA);
        CurrentExamDocument pdf = new CurrentExamDocument(
                8L, DocumentType.TESIS, DocumentKind.PDF, "tesis-subida.pdf", null, 2048L, UPLOADED_AT);
        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.GRADO))
                .thenReturn(Optional.of(new ExamRequestRead(binding, List.of(pdf))));

        ExamRequirements requirements = ready(emanuel(MAESTRIA));
        ExamRequirement thesis = item(requirements, DocumentType.TESIS);

        assertThat(requirements.request().status()).isEqualTo(ExamRequestStatus.BORRADOR);
        assertThat(requirements.request().folio()).isNull();
        assertThat(requirements.request().submittedAt()).isNull();
        assertThat(thesis.status()).isEqualTo(RequirementStatus.CARGADO);
        assertThat(thesis.currentDocument().id()).isPositive();
        assertThat(thesis.currentDocument().kind()).isEqualTo(DocumentKind.PDF);
        assertThat(thesis.currentDocument().originalName()).isNotNull();
        assertThat(thesis.currentDocument().externalUrl()).isNull();
        assertThat(thesis.currentDocument().sizeBytes()).isGreaterThanOrEqualTo(0L);
        assertThat(thesis.currentDocument()).isEqualTo(pdf);
        assertThat(Arrays.stream(CurrentExamDocument.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("storageKey", "bucket", "path", "filesystemPath", "objectKey", "filePath");
        assertThat(requirements.requirements()).filteredOn(requirement -> requirement.documentType() != DocumentType.TESIS)
                .allSatisfy(requirement -> {
                    assertThat(requirement.status()).isEqualTo(RequirementStatus.PENDIENTE);
                    assertThat(requirement.currentDocument()).isNull();
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conflictingBindings")
    @DisplayName("A request tied to another program or degree is a conflict")
    void requestTiedToAnotherProgramOrDegreeIsAConflict(String label, ExamRequestBinding binding) {
        when(readPort.findByStudentAndType(STUDENT_ID, ExamType.GRADO))
                .thenReturn(Optional.of(new ExamRequestRead(binding, List.of())));

        DegreeExamRequirementsResult result = catalog.requirements(emanuel(MAESTRIA));

        assertThat(result.error()).as(label).isEqualTo(DegreeExamRequirementsResult.ACADEMIC_PROGRAM_CHANGED);
        assertThat(result.requirements()).isNull();
        verify(readPort).findByStudentAndType(STUDENT_ID, ExamType.GRADO);
        verifyNoMoreInteractions(readPort);
    }

    private static Stream<ProgramType> nonDoctoralDegrees() {
        return Stream.of(MAESTRIA, ESPECIALIZACION);
    }

    private static Stream<Arguments> ineligibleStatuses() {
        return Stream.of(
                Arguments.of(BAJA, EligibilityReason.PROGRAM_BAJA),
                Arguments.of(SUSPENSION, EligibilityReason.PROGRAM_SUSPENSION));
    }

    private static Stream<Arguments> conflictingBindings() {
        return Stream.of(
                Arguments.of("other graduate program", binding(99L, Degree.MAESTRIA)),
                Arguments.of("other origin degree", binding(PROGRAM_ID, Degree.DOCTORADO)));
    }

    private ExamRequirements ready(StudentExamContext context) {
        return ready(context, ExamType.GRADO);
    }

    private ExamRequirements ready(StudentExamContext context, ExamType examType) {
        DegreeExamRequirementsResult result = catalog.requirements(context, examType);
        assertThat(result.error()).isNull();
        return result.requirements();
    }

    private static ExamRequirement item(ExamRequirements requirements, DocumentType documentType) {
        return requirements.requirements().stream()
                .filter(requirement -> requirement.documentType() == documentType)
                .findFirst()
                .orElseThrow();
    }

    private static StudentExamContext emanuel(ProgramType programType) {
        return emanuel(programType, EN_INVESTIGACION);
    }

    private static StudentExamContext emanuel(ProgramType programType, ProgramStatus programStatus) {
        return context(programType, programStatus, "Emanuel", "López", "Alba");
    }

    private static StudentExamContext context(
            ProgramType programType,
            ProgramStatus programStatus,
            String firstName,
            String firstLastName,
            String secondLastName) {
        return new StudentExamContext(
                STUDENT_ID, PROGRAM_ID, 100L, programType, programStatus, firstName, firstLastName, secondLastName);
    }

    private static ExamRequestBinding binding(Long graduateProgramId, Degree degree) {
        return new ExamRequestBinding(
                3L, STUDENT_ID, ExamType.GRADO, graduateProgramId, degree, ExamRequestStatus.BORRADOR, null, 1, null);
    }
}

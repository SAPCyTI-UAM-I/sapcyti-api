package mx.uam.sapcyti.exam.domain.service;

import static mx.uam.sapcyti.academic.domain.model.ProgramStatus.EN_INVESTIGACION;
import static mx.uam.sapcyti.academic.domain.model.ProgramType.DOCTORADO;
import static mx.uam.sapcyti.academic.domain.model.ProgramType.MAESTRIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.DocumentUrlFieldError;
import mx.uam.sapcyti.exam.domain.model.DocumentValidationError;
import mx.uam.sapcyti.exam.domain.model.DocumentValidationVerdict;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.PdfContentInspection;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import mx.uam.sapcyti.exam.domain.port.out.PdfContentPort;
import mx.uam.sapcyti.exam.infrastructure.adapter.out.PdfBoxContentAdapter;
import org.junit.jupiter.api.AfterEach;
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
class ExamDocumentValidatorTest {

    private static final Long STUDENT_ID = 10L;
    private static final Long PROGRAM_ID = 17L;
    private static final byte[] PDF = {1, 2, 3};
    private static final String THESIS = "Tesis_Mtria_Lopez_Alba_Emanuel.pdf";
    private static final String ARTICLE_URL = "https://revista.ejemplo.org/articulo/123";

    @Mock
    private ExamRequestReadPort readPort;

    @Mock
    private PdfContentPort pdfContentPort;

    private DegreeExamCatalog catalog;
    private ExamDocumentValidator validator;

    @BeforeEach
    void setUp() {
        catalog = new DegreeExamCatalog(readPort);
        validator = new ExamDocumentValidator(catalog, pdfContentPort, 255, 2048, 253);
    }

    @AfterEach
    void dossierIsNotRead() {
        verifyNoInteractions(readPort);
    }

    @Test
    @DisplayName("A correctly named PDF is accepted")
    void correctlyNamedPdfIsAccepted() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());
        String originalName = THESIS;

        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA), ExamType.GRADO, DocumentType.TESIS, DocumentKind.PDF, originalName, PDF, null);

        assertThat(verdict.accepted()).isTrue();
        assertThat(verdict.error()).isNull();
        assertThat(verdict.documentType()).isEqualTo(DocumentType.TESIS);
        assertThat(originalName).isEqualTo(THESIS);
        verify(pdfContentPort).inspect(PDF);
    }

    @Test
    @DisplayName("Drag and drop uses the same rules as selecting a file")
    void dragAndDropUsesTheSameRules() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());
        StudentExamContext context = emanuel(MAESTRIA);

        DocumentValidationVerdict selected = validate(
                context, ExamType.GRADO, DocumentType.TESIS, DocumentKind.PDF, THESIS, PDF, null);
        DocumentValidationVerdict dropped = validate(
                context, ExamType.GRADO, DocumentType.TESIS, DocumentKind.PDF, THESIS, PDF, null);

        assertThat(dropped).isEqualTo(selected);
        assertThat(publicValidateMethods()).containsExactly("validate");
    }

    @Test
    @DisplayName("A wrong file name is rejected with the expected name")
    void wrongFileNameIsRejectedWithTheExpectedName() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());
        String originalName = "historia.pdf";

        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA), ExamType.GRADO, DocumentType.HIST_ACAD, DocumentKind.PDF, originalName, PDF, null);

        assertThat(verdict.accepted()).isFalse();
        assertThat(verdict.error().error()).isEqualTo("INVALID_DOCUMENT_NAME");
        assertThat(verdict.error().details().expectedName()).isEqualTo("Hist_Acad_Lopez_Alba_Emanuel.pdf");
        assertThat(verdict.error().details().fieldErrors()).isNull();
        assertThat(originalName).isEqualTo("historia.pdf");
    }

    @Test
    @DisplayName("A name that belongs to another document type is rejected")
    void nameOfAnotherDocumentTypeIsRejected() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());

        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA),
                ExamType.GRADO,
                DocumentType.COMP_IDIOMA,
                DocumentKind.PDF,
                "Hist_Acad_Lopez_Alba_Emanuel.pdf",
                PDF,
                null);

        assertThat(verdict.error().error()).isEqualTo("INVALID_DOCUMENT_NAME");
        assertThat(verdict.error().details().expectedName()).isEqualTo("Comp_Idioma_Lopez_Alba_Emanuel.pdf");
    }

    @Test
    @DisplayName("A thesis name with the wrong degree token is rejected")
    void thesisNameWithTheWrongDegreeTokenIsRejected() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());

        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA),
                ExamType.GRADO,
                DocumentType.TESIS,
                DocumentKind.PDF,
                "Tesis_Docto_Lopez_Alba_Emanuel.pdf",
                PDF,
                null);

        assertThat(verdict.error().error()).isEqualTo("INVALID_DOCUMENT_NAME");
        assertThat(verdict.error().details().expectedName()).isEqualTo(THESIS);
    }

    @Test
    @DisplayName("Accents in the file name are rejected")
    void accentsInTheFileNameAreRejected() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());

        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA),
                ExamType.GRADO,
                DocumentType.TESIS,
                DocumentKind.PDF,
                "Tesis_Mtria_López_Alba_Emanuel.pdf",
                PDF,
                null);

        assertThat(verdict.error().error()).isEqualTo("INVALID_DOCUMENT_NAME");
        assertThat(verdict.error().details().expectedName()).isEqualTo(THESIS);
    }

    @Test
    @DisplayName("Letter case and an uppercase extension are ignored")
    void letterCaseAndUppercaseExtensionAreIgnored() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());
        String originalName = "TESIS_MTRIA_LOPEZ_ALBA_EMANUEL.PDF";

        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA), ExamType.GRADO, DocumentType.TESIS, DocumentKind.PDF, originalName, PDF, null);

        assertThat(verdict.accepted()).isTrue();
        assertThat(originalName).isEqualTo("TESIS_MTRIA_LOPEZ_ALBA_EMANUEL.PDF");
    }

    @Test
    @DisplayName("A missing maternal surname is omitted from the expected name")
    void missingMaternalSurnameIsOmitted() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());
        String originalName = "Hist_Acad_Lopez_Emanuel.pdf";

        DocumentValidationVerdict verdict = validate(
                context(MAESTRIA, "Emanuel", "López", null),
                ExamType.GRADO,
                DocumentType.HIST_ACAD,
                DocumentKind.PDF,
                originalName,
                PDF,
                null);

        assertThat(verdict.accepted()).isTrue();
        assertThat(catalog.officialName(context(MAESTRIA, "Emanuel", "López", null), DocumentType.HIST_ACAD).fileName())
                .isEqualTo(originalName)
                .doesNotContain("__");
    }

    @Test
    @DisplayName("A compound given name and ñ match the normalized expected name")
    void compoundGivenNameAndEnieMatch() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());

        DocumentValidationVerdict verdict = validate(
                context(MAESTRIA, "María José", "Peña", "Núñez"),
                ExamType.GRADO,
                DocumentType.HIST_ACAD,
                DocumentKind.PDF,
                "Hist_Acad_Pena_Nunez_Maria_Jose.pdf",
                PDF,
                null);

        assertThat(verdict.accepted()).isTrue();
    }

    @Test
    @DisplayName("Bytes that are not a PDF are rejected")
    void bytesThatAreNotAPdfAreRejected() {
        ExamDocumentValidator realParser = new ExamDocumentValidator(catalog, new PdfBoxContentAdapter(), 255, 2048, 253);
        byte[] disguised = "%PDF-1.4\nnot-a-pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        DocumentValidationVerdict verdict = realParser.validate(
                emanuel(MAESTRIA), ExamType.GRADO, DocumentType.TESIS, DocumentKind.PDF, THESIS, disguised, null);

        assertThat(verdict.accepted()).isFalse();
        assertThat(verdict.error().error()).isEqualTo("INVALID_PDF_CONTENT");
        assertThat(verdict.error().details()).isNull();
    }

    @Test
    @DisplayName("A PDF larger than 20 MB is accepted")
    void pdfLargerThan20MbIsAccepted() {
        byte[] large = new byte[20 * 1024 * 1024 + 1];
        when(pdfContentPort.inspect(large)).thenReturn(PdfContentInspection.ok());

        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA), ExamType.GRADO, DocumentType.TESIS, DocumentKind.PDF, THESIS, large, null);

        assertThat(verdict.accepted()).isTrue();
        assertThat(verdict.error()).isNull();
        verify(pdfContentPort).inspect(large);
    }

    @Test
    @DisplayName("An article PDF may use any safe name")
    void articlePdfMayUseAnySafeName() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());
        String originalName = "paper-final.pdf";
        StudentExamContext context = emanuel(DOCTORADO);

        DocumentValidationVerdict verdict = validate(
                context, ExamType.GRADO, DocumentType.ARTICULO, DocumentKind.PDF, originalName, PDF, null);

        assertThat(verdict.accepted()).isTrue();
        assertThat(originalName).isEqualTo("paper-final.pdf");
        assertThat(catalog.documentRule(context, ExamType.GRADO, DocumentType.ARTICULO))
                .get()
                .extracting(DegreeExamCatalog.DocumentRule::namePrescribed)
                .isEqualTo(false);
        verify(pdfContentPort).inspect(PDF);
    }

    @Test
    @DisplayName("An article may be an HTTPS URL")
    void articleMayBeAnHttpsUrl() {
        DocumentValidationVerdict verdict = validate(
                emanuel(DOCTORADO), ExamType.GRADO, DocumentType.ARTICULO, DocumentKind.URL, null, null, ARTICLE_URL);

        assertThat(verdict.accepted()).isTrue();
        assertThat(verdict.error()).isNull();
        verifyNoInteractions(pdfContentPort);
    }

    @Test
    @DisplayName("A predoctoral article uses the same PDF and URL rules")
    void predoctoralArticleUsesTheSamePdfAndUrlRules() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());
        StudentExamContext context = emanuel(DOCTORADO);

        DocumentValidationVerdict pdf = validate(
                context, ExamType.PREDOCTORAL, DocumentType.ARTICULO, DocumentKind.PDF, "paper-final.pdf", PDF, null);
        DocumentValidationVerdict url = validate(
                context, ExamType.PREDOCTORAL, DocumentType.ARTICULO, DocumentKind.URL, null, null, ARTICLE_URL);

        assertThat(pdf.accepted()).isTrue();
        assertThat(url.accepted()).isTrue();
        assertThat(catalog.documentRule(context, ExamType.PREDOCTORAL, DocumentType.ARTICULO))
                .get()
                .extracting(DegreeExamCatalog.DocumentRule::required)
                .isEqualTo(false);
        verify(pdfContentPort).inspect(PDF);
    }

    @Test
    @DisplayName("An article URL that is not absolute HTTPS is rejected")
    void articleUrlThatIsNotAbsoluteHttpsIsRejected() {
        DocumentValidationVerdict verdict = validate(
                emanuel(DOCTORADO), ExamType.GRADO, DocumentType.ARTICULO, DocumentKind.URL, null, null, "revista ejemplo");

        assertThat(verdict.accepted()).isFalse();
        assertThat(verdict.error().error()).isEqualTo("INVALID_ARTICLE_URL");
        assertThat(verdict.error().details().expectedName()).isNull();
        assertThat(verdict.error().details().fieldErrors()).containsExactly(
                DocumentUrlFieldError.externalUrl(DocumentUrlFieldError.HTTPS_REQUIRED, "Use an absolute HTTPS URL"));
        assertThat(verdict.error().message()).doesNotContain("revista ejemplo");
        verifyNoInteractions(pdfContentPort);
    }

    @ParameterizedTest(name = "{0} is {1}")
    @MethodSource("rejectedUrls")
    @DisplayName("http, credentials, fragment, empty host or another port are rejected")
    void articleUrlVariantsAreRejected(String externalUrl, String code) {
        DocumentValidationVerdict verdict = validate(
                emanuel(DOCTORADO), ExamType.GRADO, DocumentType.ARTICULO, DocumentKind.URL, null, null, externalUrl);

        assertThat(verdict.error().error()).isEqualTo("INVALID_ARTICLE_URL");
        assertThat(verdict.error().details().fieldErrors()).singleElement()
                .extracting(DocumentUrlFieldError::field, DocumentUrlFieldError::code)
                .containsExactly("externalUrl", code);
        assertThat(verdict.error().message()).doesNotContain("user:pass");
        assertThat(verdict.error().details().fieldErrors().get(0).message()).doesNotContain(externalUrl);
        verifyNoInteractions(pdfContentPort);
    }

    @Test
    @DisplayName("An unresolvable host is accepted without a lookup")
    void unresolvableHostIsAccepted() {
        DocumentValidationVerdict verdict = validate(
                emanuel(DOCTORADO),
                ExamType.GRADO,
                DocumentType.ARTICULO,
                DocumentKind.URL,
                null,
                null,
                "https://this-name-is-not-resolved.invalid/articulo");

        assertThat(verdict.accepted()).isTrue();
        verifyNoInteractions(pdfContentPort);
    }

    @Test
    @DisplayName("An explicit port 443 and an IP host are accepted")
    void explicitPortAndIpHostAreAccepted() {
        DocumentValidationVerdict port = validate(
                emanuel(DOCTORADO),
                ExamType.GRADO,
                DocumentType.ARTICULO,
                DocumentKind.URL,
                null,
                null,
                "https://revista.ejemplo.org:443/articulo/123");
        DocumentValidationVerdict ip = validate(
                emanuel(DOCTORADO),
                ExamType.GRADO,
                DocumentType.ARTICULO,
                DocumentKind.URL,
                null,
                null,
                "https://127.0.0.1/articulo");

        DocumentValidationVerdict ipv6 = validate(
                emanuel(DOCTORADO),
                ExamType.GRADO,
                DocumentType.ARTICULO,
                DocumentKind.URL,
                null,
                null,
                "https://[2001:db8::1]/articulo");

        assertThat(port.accepted()).isTrue();
        assertThat(ip.accepted()).isTrue();
        assertThat(ipv6.accepted()).isTrue();
        verifyNoInteractions(pdfContentPort);
    }

    @Test
    @DisplayName("A URL is admitted only for the article")
    void urlIsAdmittedOnlyForTheArticle() {
        DocumentValidationVerdict verdict = validate(
                emanuel(DOCTORADO), ExamType.GRADO, DocumentType.TESIS, DocumentKind.URL, null, null, ARTICLE_URL);

        assertThat(verdict.error().error()).isEqualTo("DOCUMENT_KIND_NOT_ALLOWED");
        assertThat(verdict.error().details()).isNull();
        verifyNoInteractions(pdfContentPort);
    }

    @Test
    @DisplayName("A direct call receives the same verdict")
    void directCallReceivesTheSameVerdict() {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());

        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA), ExamType.GRADO, DocumentType.HIST_ACAD, DocumentKind.PDF, "historia.pdf", PDF, null);

        assertThat(verdict.error().error()).isEqualTo("INVALID_DOCUMENT_NAME");
        assertThat(verdict.error().details().expectedName()).isEqualTo("Hist_Acad_Lopez_Alba_Emanuel.pdf");
        assertThat(publicValidateMethods()).containsExactly("validate");
    }

    @Test
    @DisplayName("A file name that carries a path is rejected")
    void fileNameThatCarriesAPathIsRejected() {
        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA),
                ExamType.GRADO,
                DocumentType.TESIS,
                DocumentKind.PDF,
                "../Tesis_Mtria_Lopez_Alba_Emanuel.pdf",
                PDF,
                null);

        assertThat(verdict.error().error()).isEqualTo("UNSAFE_FILE_NAME");
        assertThat(verdict.error().details()).isNull();
        assertThat(Arrays.stream(DocumentValidationVerdict.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("documentType", "accepted", "error");
        verifyNoInteractions(pdfContentPort);
    }

    @Test
    @DisplayName("An unsafe article name is rejected")
    void unsafeArticleNameIsRejected() {
        DocumentValidationVerdict verdict = validate(
                emanuel(DOCTORADO),
                ExamType.GRADO,
                DocumentType.ARTICULO,
                DocumentKind.PDF,
                "paper/final.pdf",
                PDF,
                null);

        assertThat(verdict.error().error()).isEqualTo("UNSAFE_FILE_NAME");
        assertThat(verdict.error().details()).isNull();
        verifyNoInteractions(pdfContentPort);
    }

    @Test
    @DisplayName("A file name longer than the configured limit is rejected")
    void fileNameLongerThanTheConfiguredLimitIsRejected() {
        ExamDocumentValidator tight = new ExamDocumentValidator(catalog, pdfContentPort, 8, 2048, 253);

        DocumentValidationVerdict verdict = tight.validate(
                emanuel(DOCTORADO),
                ExamType.GRADO,
                DocumentType.ARTICULO,
                DocumentKind.PDF,
                "paper-final.pdf",
                PDF,
                null);

        assertThat(verdict.error().error()).isEqualTo("UNSAFE_FILE_NAME");
        verifyNoInteractions(pdfContentPort);
    }

    @ParameterizedTest(name = "missing {0}")
    @MethodSource("incompleteNames")
    @DisplayName("A missing official given name or paternal surname cannot build a prescribed name")
    void missingOfficialNameCannotBuildAPrescribedName(String firstName, String firstLastName) {
        when(pdfContentPort.inspect(PDF)).thenReturn(PdfContentInspection.ok());

        DocumentValidationVerdict verdict = validate(
                context(MAESTRIA, firstName, firstLastName, "Alba"),
                ExamType.GRADO,
                DocumentType.TESIS,
                DocumentKind.PDF,
                THESIS,
                PDF,
                null);

        assertThat(verdict.error().error()).isEqualTo(DocumentValidationError.STUDENT_DATA_INCOMPLETE);
        assertThat(verdict.error().details()).isNull();
        assertThat(verdict.error().message()).doesNotContain("null");
    }

    @Test
    @DisplayName("A document type from another degree is not admitted")
    void documentTypeFromAnotherDegreeIsNotAdmitted() {
        DocumentValidationVerdict verdict = validate(
                emanuel(MAESTRIA), ExamType.GRADO, DocumentType.ARTICULO, DocumentKind.PDF, "paper-final.pdf", PDF, null);

        assertThat(verdict.error().error()).isEqualTo("DOCUMENT_KIND_NOT_ALLOWED");
        assertThat(verdict.error().details()).isNull();
        verifyNoInteractions(pdfContentPort);
    }

    private DocumentValidationVerdict validate(
            StudentExamContext context,
            ExamType examType,
            DocumentType documentType,
            DocumentKind kind,
            String originalName,
            byte[] pdfBytes,
            String externalUrl) {
        return validator.validate(context, examType, documentType, kind, originalName, pdfBytes, externalUrl);
    }

    private static java.util.stream.Stream<Arguments> rejectedUrls() {
        return java.util.stream.Stream.of(
                Arguments.of("http://revista.ejemplo.org/articulo/123", DocumentUrlFieldError.HTTPS_REQUIRED),
                Arguments.of("javascript:alert(1)", DocumentUrlFieldError.HTTPS_REQUIRED),
                Arguments.of("data:text/plain,hi", DocumentUrlFieldError.HTTPS_REQUIRED),
                Arguments.of("/articulo/123", DocumentUrlFieldError.HTTPS_REQUIRED),
                Arguments.of("https://user:pass@revista.ejemplo.org/a", DocumentUrlFieldError.INVALID_VALUE),
                Arguments.of("https://revista.ejemplo.org/a#frag", DocumentUrlFieldError.INVALID_VALUE),
                Arguments.of("https://", DocumentUrlFieldError.INVALID_VALUE),
                Arguments.of("https://revista.ejemplo.org:8443/a", DocumentUrlFieldError.INVALID_VALUE));
    }

    private static java.util.stream.Stream<Arguments> incompleteNames() {
        return java.util.stream.Stream.of(
                Arguments.of(null, "López"),
                Arguments.of("Emanuel", " "));
    }

    private static java.util.List<String> publicValidateMethods() {
        return Arrays.stream(ExamDocumentValidator.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .filter(name -> name.startsWith("validate"))
                .toList();
    }

    private static StudentExamContext emanuel(ProgramType programType) {
        return context(programType, "Emanuel", "López", "Alba");
    }

    private static StudentExamContext context(
            ProgramType programType, String firstName, String firstLastName, String secondLastName) {
        return new StudentExamContext(
                STUDENT_ID, PROGRAM_ID, 100L, programType, EN_INVESTIGACION, firstName, firstLastName, secondLastName);
    }
}

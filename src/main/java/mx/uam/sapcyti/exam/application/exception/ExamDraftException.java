package mx.uam.sapcyti.exam.application.exception;

import java.util.List;
import mx.uam.sapcyti.exam.domain.model.DocumentUrlFieldError;

/**
 * Failure of the HU-65 draft writes and panel. {@code VALIDATION_ERROR}, {@code INVALID_ARTICLE_URL}
 * and {@code VERSION_CONFLICT} carry field errors. {@code INVALID_DOCUMENT_NAME} carries
 * {@code expectedName}. Every other code is published without {@code details}.
 */
public class ExamDraftException extends RuntimeException {

    /**
     * Wire codes of these routes. Individual batch results are not members: a rejected descriptor
     * stays inside HTTP 200.
     */
    public enum Code {
        FORBIDDEN,
        PROGRAM_NOT_ELIGIBLE,
        EXAM_TYPE_NOT_ALLOWED,
        STUDENT_NOT_FOUND,
        REQUEST_NOT_FOUND,
        DOCUMENT_NOT_FOUND,
        VERSION_CONFLICT,
        REQUEST_NOT_DRAFT,
        ACADEMIC_PROGRAM_AMBIGUOUS,
        ACADEMIC_PROGRAM_CHANGED,
        STUDENT_DATA_INCOMPLETE,
        VALIDATION_ERROR,
        REQUEST_TOO_LARGE,
        UNSUPPORTED_MEDIA_TYPE,
        UNSAFE_FILE_NAME,
        INVALID_DOCUMENT_NAME,
        INVALID_PDF_CONTENT,
        INVALID_ARTICLE_URL,
        DOCUMENT_KIND_NOT_ALLOWED,
        STORAGE_UNAVAILABLE,
        DEPENDENCY_UNAVAILABLE
    }

    private final Code code;
    private final List<ExamDraftFieldError> fieldErrors;
    private final String expectedName;

    private ExamDraftException(
            Code code, String message, List<ExamDraftFieldError> fieldErrors, String expectedName) {
        super(message);
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
        this.expectedName = expectedName;
    }

    public Code code() {
        return code;
    }

    public List<ExamDraftFieldError> fieldErrors() {
        return fieldErrors;
    }

    public String expectedName() {
        return expectedName;
    }

    public static ExamDraftException forbidden() {
        return plain(Code.FORBIDDEN, "Access denied");
    }

    public static ExamDraftException programNotEligible() {
        return plain(Code.PROGRAM_NOT_ELIGIBLE, "The academic status does not allow saving documents");
    }

    public static ExamDraftException examTypeNotAllowed() {
        return plain(Code.EXAM_TYPE_NOT_ALLOWED, "Predoctoral is available only for doctoral students");
    }

    public static ExamDraftException studentNotFound() {
        return plain(Code.STUDENT_NOT_FOUND, "Student program was not found");
    }

    public static ExamDraftException requestNotFound() {
        return plain(Code.REQUEST_NOT_FOUND, "Exam request was not found");
    }

    public static ExamDraftException documentNotFound() {
        return plain(Code.DOCUMENT_NOT_FOUND, "Current exam document was not found");
    }

    public static ExamDraftException versionConflict() {
        return new ExamDraftException(
                Code.VERSION_CONFLICT,
                "The request was changed by another operation",
                List.of(new ExamDraftFieldError("version", "STALE_VERSION", "Reload the request")),
                null);
    }

    public static ExamDraftException requestNotDraft() {
        return plain(Code.REQUEST_NOT_DRAFT, "The request is no longer a draft");
    }

    public static ExamDraftException academicProgramAmbiguous() {
        return plain(Code.ACADEMIC_PROGRAM_AMBIGUOUS, "Academic program is ambiguous");
    }

    public static ExamDraftException academicProgramChanged() {
        return plain(Code.ACADEMIC_PROGRAM_CHANGED, "Academic program changed");
    }

    public static ExamDraftException studentDataIncomplete() {
        return plain(Code.STUDENT_DATA_INCOMPLETE, "Student data is incomplete");
    }

    public static ExamDraftException validation(String message, List<ExamDraftFieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            throw new IllegalArgumentException("VALIDATION_ERROR requires field errors");
        }
        return new ExamDraftException(Code.VALIDATION_ERROR, message, fieldErrors, null);
    }

    public static ExamDraftException requestTooLarge() {
        return plain(Code.REQUEST_TOO_LARGE, "The request is too large");
    }

    public static ExamDraftException unsupportedMediaType() {
        return plain(Code.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
    }

    public static ExamDraftException unsafeFileName(String message) {
        return plain(Code.UNSAFE_FILE_NAME, message);
    }

    public static ExamDraftException invalidDocumentName(String message, String expectedName) {
        return new ExamDraftException(Code.INVALID_DOCUMENT_NAME, message, List.of(), expectedName);
    }

    public static ExamDraftException invalidPdfContent(String message) {
        return plain(Code.INVALID_PDF_CONTENT, message);
    }

    public static ExamDraftException invalidArticleUrl(String message, List<DocumentUrlFieldError> fieldErrors) {
        List<ExamDraftFieldError> published = fieldErrors.stream()
                .map(error -> new ExamDraftFieldError(error.field(), error.code(), error.message()))
                .toList();
        return new ExamDraftException(Code.INVALID_ARTICLE_URL, message, published, null);
    }

    public static ExamDraftException documentKindNotAllowed(String message) {
        return plain(Code.DOCUMENT_KIND_NOT_ALLOWED, message);
    }

    public static ExamDraftException storageUnavailable() {
        return plain(Code.STORAGE_UNAVAILABLE, "The document could not be stored");
    }

    public static ExamDraftException dependencyUnavailable() {
        return plain(Code.DEPENDENCY_UNAVAILABLE, "The draft could not be saved");
    }

    private static ExamDraftException plain(Code code, String message) {
        return new ExamDraftException(code, message, List.of(), null);
    }
}

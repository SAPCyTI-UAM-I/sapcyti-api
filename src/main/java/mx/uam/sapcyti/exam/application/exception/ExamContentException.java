package mx.uam.sapcyti.exam.application.exception;

import java.util.List;

/**
 * Failure of the HU-66 content read. Only {@code VALIDATION_ERROR} carries field errors.
 * Every other code is published without {@code details}. These codes do not widen
 * {@link ExamDraftException.Code}.
 */
public class ExamContentException extends RuntimeException {

    /**
     * Wire codes of {@code GET .../documents/{documentType}/content}.
     */
    public enum Code {
        VALIDATION_ERROR,
        UNAUTHORIZED,
        FORBIDDEN,
        DOCUMENT_NOT_FOUND,
        DOCUMENT_IS_EXTERNAL_URL,
        DOCUMENT_STORAGE_UNAVAILABLE
    }

    private final Code code;
    private final List<ExamContentFieldError> fieldErrors;

    private ExamContentException(Code code, String message, List<ExamContentFieldError> fieldErrors) {
        super(message);
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public Code code() {
        return code;
    }

    public List<ExamContentFieldError> fieldErrors() {
        return fieldErrors;
    }

    public static ExamContentException forbidden() {
        return plain(Code.FORBIDDEN, "Access denied");
    }

    public static ExamContentException notFound() {
        return plain(Code.DOCUMENT_NOT_FOUND, "Document not found");
    }

    public static ExamContentException externalUrl() {
        return plain(Code.DOCUMENT_IS_EXTERNAL_URL, "Document is an external URL");
    }

    public static ExamContentException storageUnavailable() {
        return plain(Code.DOCUMENT_STORAGE_UNAVAILABLE, "Document storage is unavailable");
    }

    public static ExamContentException validation(String message, List<ExamContentFieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            throw new IllegalArgumentException("VALIDATION_ERROR requires field errors");
        }
        return new ExamContentException(Code.VALIDATION_ERROR, message, fieldErrors);
    }

    private static ExamContentException plain(Code code, String message) {
        return new ExamContentException(code, message, List.of());
    }
}

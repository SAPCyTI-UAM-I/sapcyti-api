package mx.uam.sapcyti.exam.domain.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentKindAdmission;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.DocumentUrlFieldError;
import mx.uam.sapcyti.exam.domain.model.DocumentValidationDetails;
import mx.uam.sapcyti.exam.domain.model.DocumentValidationError;
import mx.uam.sapcyti.exam.domain.model.DocumentValidationErrorCode;
import mx.uam.sapcyti.exam.domain.model.DocumentValidationStep;
import mx.uam.sapcyti.exam.domain.model.DocumentValidationVerdict;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.OfficialDocumentName;
import mx.uam.sapcyti.exam.domain.model.PdfContentInspection;
import mx.uam.sapcyti.exam.domain.port.out.PdfContentPort;
import mx.uam.sapcyti.exam.domain.service.DegreeExamCatalog.DocumentRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Accepts or rejects one PDF or article link before it is stored (SPEC-050).
 *
 * <p>The caller passes an already resolved {@link StudentExamContext}. This service does not
 * resolve the student, read the dossier, open the bucket, or fetch the URL. Drag-and-drop and a
 * direct call use this same method. The received name is not rewritten.
 */
@Service
public class ExamDocumentValidator {

    private static final String NAME_MISMATCH = "Document name does not match";
    private static final String UNSAFE_NAME = "File name is not safe";
    private static final String KIND_NOT_ALLOWED = "This document is not allowed";
    private static final String PDF_REJECTED = "The file must be a valid PDF";
    private static final String INCOMPLETE = "Student data is incomplete";
    private static final String URL_REJECTED = "A valid HTTPS URL is required";
    private static final String URL_HTTPS = "Use an absolute HTTPS URL";
    private static final String URL_REQUIRED = "An article URL is required";
    private static final String URL_INVALID = "The URL is not acceptable";

    private final DegreeExamCatalog degreeExamCatalog;
    private final PdfContentPort pdfContentPort;
    private final int maxOriginalNameBytes;
    private final int maxUrlLength;
    private final int maxHostLength;

    public ExamDocumentValidator(
            DegreeExamCatalog degreeExamCatalog,
            PdfContentPort pdfContentPort,
            @Value("${app.exam.documents.max-original-name-bytes:255}") int maxOriginalNameBytes,
            @Value("${app.exam.documents.max-url-length:2048}") int maxUrlLength,
            @Value("${app.exam.documents.max-host-length:253}") int maxHostLength) {
        this.degreeExamCatalog = Objects.requireNonNull(degreeExamCatalog, "degreeExamCatalog");
        this.pdfContentPort = Objects.requireNonNull(pdfContentPort, "pdfContentPort");
        if (maxOriginalNameBytes < 1 || maxUrlLength < 1 || maxHostLength < 1) {
            throw new IllegalArgumentException("document validation limits must be positive");
        }
        this.maxOriginalNameBytes = maxOriginalNameBytes;
        this.maxUrlLength = maxUrlLength;
        this.maxHostLength = maxHostLength;
    }

    /**
     * Validates one document. For {@link DocumentKind#PDF} the name and bytes are judged.
     * For {@link DocumentKind#URL} only {@code externalUrl} is judged, and no object is written.
     */
    public DocumentValidationVerdict validate(
            StudentExamContext context,
            ExamType examType,
            DocumentType documentType,
            DocumentKind kind,
            String originalName,
            byte[] pdfBytes,
            String externalUrl) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(examType, "examType");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(kind, "kind");
        Optional<DocumentRule> rule = degreeExamCatalog.documentRule(context, examType, documentType);
        for (DocumentValidationStep step : DocumentValidationStep.values()) {
            DocumentValidationError error = apply(
                    step, context, examType, documentType, kind, originalName, pdfBytes, externalUrl, rule);
            if (error != null) {
                return DocumentValidationVerdict.rejected(documentType, error);
            }
        }
        return DocumentValidationVerdict.accepted(documentType);
    }

    private DocumentValidationError apply(
            DocumentValidationStep step,
            StudentExamContext context,
            ExamType examType,
            DocumentType documentType,
            DocumentKind kind,
            String originalName,
            byte[] pdfBytes,
            String externalUrl,
            Optional<DocumentRule> rule) {
        return switch (step) {
            case KIND_ADMISSION -> kindAdmission(context, examType, documentType, kind);
            case SAFE_NAME -> kind == DocumentKind.PDF ? safeName(originalName) : null;
            case PDF_CONTENT -> kind == DocumentKind.PDF ? pdfContent(pdfBytes) : null;
            case PRESCRIBED_NAME -> prescribedName(context, documentType, kind, originalName, rule);
            case ARTICLE_URL -> kind == DocumentKind.URL ? articleUrl(externalUrl) : null;
        };
    }

    private DocumentValidationError kindAdmission(
            StudentExamContext context, ExamType examType, DocumentType documentType, DocumentKind kind) {
        DocumentKindAdmission admission = degreeExamCatalog.kindAdmission(context, examType, documentType, kind);
        if (admission.admitted()) {
            return null;
        }
        return error(DocumentValidationErrorCode.DOCUMENT_KIND_NOT_ALLOWED, KIND_NOT_ALLOWED, null);
    }

    private DocumentValidationError safeName(String originalName) {
        if (isSafeOriginalName(originalName)) {
            return null;
        }
        return error(DocumentValidationErrorCode.UNSAFE_FILE_NAME, UNSAFE_NAME, null);
    }

    private DocumentValidationError pdfContent(byte[] pdfBytes) {
        PdfContentInspection inspection = pdfContentPort.inspect(pdfBytes);
        if (inspection.accepted()) {
            return null;
        }
        return error(DocumentValidationErrorCode.INVALID_PDF_CONTENT, PDF_REJECTED, null);
    }

    private DocumentValidationError prescribedName(
            StudentExamContext context,
            DocumentType documentType,
            DocumentKind kind,
            String originalName,
            Optional<DocumentRule> rule) {
        if (kind != DocumentKind.PDF || rule.isEmpty() || !rule.get().namePrescribed()) {
            return null;
        }
        final OfficialDocumentName official;
        try {
            official = degreeExamCatalog.officialName(context, documentType);
        } catch (IllegalArgumentException ex) {
            return new DocumentValidationError(DocumentValidationError.STUDENT_DATA_INCOMPLETE, INCOMPLETE, null);
        }
        String expected = official.fileName();
        if (expected == null) {
            return new DocumentValidationError(DocumentValidationError.STUDENT_DATA_INCOMPLETE, INCOMPLETE, null);
        }
        if (sameIgnoringCase(originalName, expected)) {
            return null;
        }
        return error(
                DocumentValidationErrorCode.INVALID_DOCUMENT_NAME,
                NAME_MISMATCH,
                DocumentValidationDetails.expectedName(expected));
    }

    private DocumentValidationError articleUrl(String externalUrl) {
        DocumentUrlFieldError fieldError = urlFieldError(externalUrl);
        if (fieldError == null) {
            return null;
        }
        return error(
                DocumentValidationErrorCode.INVALID_ARTICLE_URL,
                URL_REJECTED,
                DocumentValidationDetails.fieldErrors(List.of(fieldError)));
    }

    private DocumentUrlFieldError urlFieldError(String value) {
        if (value == null || value.isBlank()) {
            return DocumentUrlFieldError.externalUrl(DocumentUrlFieldError.REQUIRED, URL_REQUIRED);
        }
        if (!value.regionMatches(true, 0, "https:", 0, "https:".length())) {
            return DocumentUrlFieldError.externalUrl(DocumentUrlFieldError.HTTPS_REQUIRED, URL_HTTPS);
        }
        if (hasControlOrSpace(value) || utf8Length(value) > maxUrlLength) {
            return DocumentUrlFieldError.externalUrl(DocumentUrlFieldError.INVALID_VALUE, URL_INVALID);
        }
        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException ex) {
            return DocumentUrlFieldError.externalUrl(DocumentUrlFieldError.INVALID_VALUE, URL_INVALID);
        }
        if (!uri.isAbsolute() || uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            return DocumentUrlFieldError.externalUrl(DocumentUrlFieldError.HTTPS_REQUIRED, URL_HTTPS);
        }
        if (uri.getRawUserInfo() != null || uri.getRawFragment() != null) {
            return DocumentUrlFieldError.externalUrl(DocumentUrlFieldError.INVALID_VALUE, URL_INVALID);
        }
        String host = uri.getHost();
        if (host != null && host.startsWith("[") && host.endsWith("]") && host.length() > 2) {
            host = host.substring(1, host.length() - 1);
        }
        if (host == null || host.isBlank() || utf8Length(host) > maxHostLength || !validHost(host)) {
            return DocumentUrlFieldError.externalUrl(DocumentUrlFieldError.INVALID_VALUE, URL_INVALID);
        }
        int port = uri.getPort();
        if (port != -1 && port != 443) {
            return DocumentUrlFieldError.externalUrl(DocumentUrlFieldError.INVALID_VALUE, URL_INVALID);
        }
        return null;
    }

    private boolean isSafeOriginalName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.contains("..")) {
            return false;
        }
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(name)) {
            return false;
        }
        if (name.codePoints().anyMatch(Character::isISOControl)) {
            return false;
        }
        return utf8Length(name) <= maxOriginalNameBytes;
    }

    private static boolean sameIgnoringCase(String received, String expected) {
        int[] left = received.codePoints().map(Character::toLowerCase).toArray();
        int[] right = expected.codePoints().map(Character::toLowerCase).toArray();
        return Arrays.equals(left, right);
    }

    private static boolean hasControlOrSpace(String value) {
        return value.codePoints().anyMatch(code -> Character.isISOControl(code) || Character.isWhitespace(code));
    }

    private static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Host syntax only. A DNS name is not looked up and an address literal is not resolved.
     */
    private static boolean validHost(String host) {
        String trimmed = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
        if (trimmed.isEmpty()) {
            return false;
        }
        if (isIpv4(trimmed)) {
            return true;
        }
        if (trimmed.indexOf(':') >= 0) {
            return isIpv6Literal(trimmed);
        }
        return isDnsName(trimmed);
    }

    private static boolean isIpv6Literal(String host) {
        int first = host.indexOf("::");
        if (first != host.lastIndexOf("::")) {
            return false;
        }
        String[] halves = first >= 0
                ? new String[] {host.substring(0, first), host.substring(first + 2)}
                : new String[] {host};
        int count = 0;
        for (String half : halves) {
            if (half.isEmpty()) {
                continue;
            }
            String[] groups = half.split(":", -1);
            for (String group : groups) {
                if (group.indexOf('.') >= 0) {
                    if (!isIpv4(group)) {
                        return false;
                    }
                    count += 2;
                } else if (!isHexGroup(group)) {
                    return false;
                } else {
                    count++;
                }
            }
        }
        return first >= 0 ? count < 8 : count == 8;
    }

    private static boolean isHexGroup(String group) {
        if (group.isEmpty() || group.length() > 4) {
            return false;
        }
        for (int index = 0; index < group.length(); index++) {
            char ch = group.charAt(index);
            boolean hex = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIpv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || (part.length() > 1 && part.charAt(0) == '0')) {
                return false;
            }
            int value = 0;
            for (int index = 0; index < part.length(); index++) {
                char ch = part.charAt(index);
                if (ch < '0' || ch > '9') {
                    return false;
                }
                value = value * 10 + (ch - '0');
            }
            if (value > 255) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDnsName(String host) {
        if (host.length() > 253) {
            return false;
        }
        String[] labels = host.split("\\.", -1);
        if (labels.length == 0) {
            return false;
        }
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-")) {
                return false;
            }
            for (int index = 0; index < label.length(); index++) {
                char ch = label.charAt(index);
                boolean letter = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
                boolean digit = ch >= '0' && ch <= '9';
                if (!letter && !digit && ch != '-') {
                    return false;
                }
            }
        }
        return true;
    }

    private static DocumentValidationError error(
            DocumentValidationErrorCode code, String message, DocumentValidationDetails details) {
        return new DocumentValidationError(code.name(), message, details);
    }
}

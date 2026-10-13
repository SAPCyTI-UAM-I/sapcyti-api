package mx.uam.sapcyti.exam.infrastructure.adapter.in;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import mx.uam.sapcyti.exam.application.exception.ExamDraftException;
import mx.uam.sapcyti.exam.application.exception.ExamDraftFieldError;
import mx.uam.sapcyti.exam.application.model.DraftCommands;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import org.springframework.stereotype.Component;

/**
 * Reads the HU-65 envelope. An invalid manifest, part or version throws before the use case
 * and therefore writes nothing. A descriptor that fails document validation is not decided here.
 */
@Component
public class ExamDraftHttpReader {

    private static final Set<String> MANIFEST_FIELDS = Set.of("version", "documents");
    private static final Set<String> PDF_FIELDS = Set.of("documentType", "kind", "filePart");
    private static final Set<String> URL_FIELDS = Set.of("documentType", "kind", "externalUrl");
    private static final Set<String> URL_BODY_FIELDS = Set.of("version", "kind", "externalUrl");
    private static final Set<String> DELETE_FIELDS = Set.of("version");
    private static final String FILE_PART = "^file_(TESIS|ARTICULO|SOL_SINODALES|HIST_ACAD|COMP_IDIOMA|DATOS_ALUM)$";

    private final ObjectMapper objectMapper;

    public ExamDraftHttpReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DraftCommands.Batch readBatch(HttpServletRequest request) {
        rejectQuery(request);
        if (!isMultipartForm(request)) {
            throw ExamDraftException.unsupportedMediaType();
        }
        List<Uploaded> parts = parts(request);
        Uploaded manifest = sole(parts, "manifest");
        if (manifest == null) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("manifest", "REQUIRED", "Manifest is required")));
        }
        if (!isJson(manifest.contentType())) {
            throw ExamDraftException.unsupportedMediaType();
        }
        JsonNode root = object(manifest.bytes(), "manifest");
        rejectUnknown(root, MANIFEST_FIELDS, "manifest");
        int version = requiredVersion(root.get("version"), 0);
        List<DraftCommands.Document> documents = documents(root.get("documents"));
        rejectBatchParts(parts, documents);
        return new DraftCommands.Batch(version, documents);
    }

    public DraftCommands.PdfReplacement readPdfReplacement(HttpServletRequest request) {
        rejectQuery(request);
        if (!isMultipartForm(request)) {
            throw ExamDraftException.unsupportedMediaType();
        }
        List<Uploaded> parts = parts(request);
        rejectExactNames(parts, Set.of("version", "file"));
        Uploaded versionPart = sole(parts, "version");
        Uploaded file = sole(parts, "file");
        List<ExamDraftFieldError> errors = new ArrayList<>();
        if (versionPart == null) {
            errors.add(new ExamDraftFieldError("version", "REQUIRED", "Version is required"));
        }
        if (file == null) {
            errors.add(new ExamDraftFieldError("file", "REQUIRED", "File is required"));
        }
        if (!errors.isEmpty()) {
            throw ExamDraftException.validation("Invalid request", errors);
        }
        int version = textVersion(versionPart.bytes());
        return new DraftCommands.PdfReplacement(version, file.filename(), file.bytes());
    }

    public DraftCommands.UrlReplacement readUrlReplacement(HttpServletRequest request) {
        rejectQuery(request);
        if (!isJson(request.getContentType())) {
            throw ExamDraftException.unsupportedMediaType();
        }
        JsonNode root = object(readBody(request), "body");
        rejectUnknown(root, URL_BODY_FIELDS, "body");
        int version = requiredVersion(root.get("version"), 1);
        requireUrlKind(root.get("kind"));
        String externalUrl = requiredText(root.get("externalUrl"), "externalUrl");
        return new DraftCommands.UrlReplacement(version, externalUrl);
    }

    public int readDeleteVersion(HttpServletRequest request) {
        rejectQuery(request);
        if (!isJson(request.getContentType())) {
            throw ExamDraftException.unsupportedMediaType();
        }
        JsonNode root = object(readBody(request), "body");
        rejectUnknown(root, DELETE_FIELDS, "body");
        return requiredVersion(root.get("version"), 1);
    }

    private List<DraftCommands.Document> documents(JsonNode node) {
        if (node == null || node.isNull()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("documents", "REQUIRED", "Documents are required")));
        }
        if (!node.isArray()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("documents", "INVALID_TYPE", "Documents must be a list")));
        }
        if (node.isEmpty()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("documents", "INVALID_VALUE", "Select at least one document")));
        }
        if (node.size() > 6) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("documents", "INVALID_VALUE", "Too many documents")));
        }
        List<DraftCommands.Document> documents = new ArrayList<>();
        Set<DocumentType> seen = new LinkedHashSet<>();
        List<ExamDraftFieldError> errors = new ArrayList<>();
        for (int index = 0; index < node.size(); index++) {
            documents.add(descriptor(node.get(index), index, seen, errors));
        }
        if (!errors.isEmpty()) {
            throw ExamDraftException.validation("Invalid request", errors);
        }
        return documents;
    }

    private DraftCommands.Document descriptor(
            JsonNode node, int index, Set<DocumentType> seen, List<ExamDraftFieldError> errors) {
        String prefix = "documents[" + index + "]";
        if (node == null || !node.isObject()) {
            errors.add(new ExamDraftFieldError(prefix, "INVALID_TYPE", "Document must be an object"));
            return null;
        }
        DocumentType documentType = enumValue(
                node.get("documentType"), prefix + ".documentType", DocumentType.class, errors);
        DocumentKind kind = enumValue(node.get("kind"), prefix + ".kind", DocumentKind.class, errors);
        if (documentType != null && !seen.add(documentType)) {
            errors.add(new ExamDraftFieldError(
                    prefix + ".documentType", "INVALID_VALUE", "Document type is repeated"));
        }
        if (kind == null || documentType == null) {
            return null;
        }
        Set<String> allowed = kind == DocumentKind.PDF ? PDF_FIELDS : URL_FIELDS;
        if (unknown(node, allowed)) {
            errors.add(new ExamDraftFieldError(prefix, "INVALID_VALUE", "Document has an unsupported property"));
            return null;
        }
        if (kind == DocumentKind.PDF) {
            String filePart = textOrNull(node.get("filePart"));
            String expected = "file_" + documentType.name();
            if (filePart == null) {
                errors.add(new ExamDraftFieldError(
                        prefix + ".filePart", "REQUIRED", "File part is required"));
            } else if (!expected.equals(filePart)) {
                errors.add(new ExamDraftFieldError(
                        prefix + ".filePart", "INVALID_VALUE", "File part does not match the document type"));
            }
            return new DraftCommands.Document(documentType, kind, null, null, null);
        }
        String externalUrl = textOrNull(node.get("externalUrl"));
        if (externalUrl == null) {
            errors.add(new ExamDraftFieldError("externalUrl", "REQUIRED", "An article URL is required"));
            return null;
        }
        if (externalUrl.isBlank()) {
            errors.add(new ExamDraftFieldError("externalUrl", "INVALID_VALUE", "An article URL is required"));
            return null;
        }
        return new DraftCommands.Document(documentType, kind, null, null, externalUrl);
    }

    private void rejectBatchParts(List<Uploaded> parts, List<DraftCommands.Document> documents) {
        Set<String> expected = new LinkedHashSet<>();
        expected.add("manifest");
        List<DraftCommands.Document> withBytes = new ArrayList<>();
        for (DraftCommands.Document document : documents) {
            if (document != null && document.kind() == DocumentKind.PDF) {
                String name = "file_" + document.documentType().name();
                expected.add(name);
                withBytes.add(document);
            }
        }
        List<ExamDraftFieldError> errors = new ArrayList<>();
        Set<String> present = new LinkedHashSet<>();
        for (Uploaded part : parts) {
            String name = part.name();
            if (!present.add(name)) {
                errors.add(fieldForPart(name, "INVALID_VALUE", "Part is repeated"));
                continue;
            }
            if (!expected.contains(name)) {
                errors.add(fieldForPart(name, "INVALID_VALUE", "Part is not part of the manifest"));
            }
        }
        for (String name : expected) {
            if (!present.contains(name)) {
                errors.add(fieldForPart(name, "REQUIRED", "Part is required"));
            }
        }
        if (!errors.isEmpty()) {
            throw ExamDraftException.validation("Invalid request", errors);
        }
        List<DraftCommands.Document> loaded = new ArrayList<>();
        for (DraftCommands.Document document : documents) {
            if (document.kind() != DocumentKind.PDF) {
                loaded.add(document);
                continue;
            }
            Uploaded part = sole(parts, "file_" + document.documentType().name());
            loaded.add(new DraftCommands.Document(
                    document.documentType(),
                    document.kind(),
                    part.filename(),
                    part.bytes(),
                    null));
        }
        documents.clear();
        documents.addAll(loaded);
        if (withBytes.size() != loaded.stream().filter(document -> document.kind() == DocumentKind.PDF).count()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("documents", "INVALID_VALUE", "File parts do not match")));
        }
    }

    private static ExamDraftFieldError fieldForPart(String name, String code, String message) {
        if ("manifest".equals(name) || (name != null && name.matches(FILE_PART))) {
            return new ExamDraftFieldError(name, code, message);
        }
        return new ExamDraftFieldError("body", code, message);
    }

    private static void rejectExactNames(List<Uploaded> parts, Set<String> allowed) {
        List<ExamDraftFieldError> errors = new ArrayList<>();
        Set<String> present = new LinkedHashSet<>();
        for (Uploaded part : parts) {
            String name = part.name();
            if (!present.add(name) || !allowed.contains(name)) {
                errors.add(new ExamDraftFieldError("body", "INVALID_VALUE", "Unexpected part"));
            }
        }
        if (!errors.isEmpty()) {
            throw ExamDraftException.validation("Invalid request", errors);
        }
    }

    private int textVersion(byte[] raw) {
        String text = new String(raw, StandardCharsets.US_ASCII);
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(text) || !text.equals(new String(raw, StandardCharsets.UTF_8))) {
            throw ExamDraftException.validation(
                    "Invalid version",
                    List.of(new ExamDraftFieldError("version", "INVALID_TYPE", "Version must be a decimal integer")));
        }
        if (!text.matches("[0-9]+") || (text.length() > 1 && text.charAt(0) == '0')) {
            throw ExamDraftException.validation(
                    "Invalid version",
                    List.of(new ExamDraftFieldError("version", "INVALID_TYPE", "Version must be a decimal integer")));
        }
        try {
            int version = Integer.parseInt(text);
            if (version < 1) {
                throw ExamDraftException.validation(
                        "Invalid version",
                        List.of(new ExamDraftFieldError("version", "OUT_OF_RANGE", "Version must be at least 1")));
            }
            return version;
        } catch (NumberFormatException ex) {
            throw ExamDraftException.validation(
                    "Invalid version",
                    List.of(new ExamDraftFieldError("version", "OUT_OF_RANGE", "Version is out of range")));
        }
    }

    private int requiredVersion(JsonNode node, int minimum) {
        if (node == null || node.isNull()) {
            throw ExamDraftException.validation(
                    "Invalid version",
                    List.of(new ExamDraftFieldError("version", "REQUIRED", "Version is required")));
        }
        if (!canonicalInteger(node)) {
            throw ExamDraftException.validation(
                    "Invalid version",
                    List.of(new ExamDraftFieldError("version", "INVALID_TYPE", "Version must be an integer")));
        }
        if (!node.canConvertToInt()) {
            throw ExamDraftException.validation(
                    "Invalid version",
                    List.of(new ExamDraftFieldError("version", "OUT_OF_RANGE", "Version is out of range")));
        }
        int version = node.intValue();
        if (version < minimum) {
            throw ExamDraftException.validation(
                    "Invalid version",
                    List.of(new ExamDraftFieldError("version", "OUT_OF_RANGE", "Version is out of range")));
        }
        return version;
    }

    private static void requireUrlKind(JsonNode node) {
        if (node == null || node.isNull()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("kind", "REQUIRED", "Kind is required")));
        }
        if (!node.isTextual()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("kind", "INVALID_TYPE", "Kind must be URL")));
        }
        if (!"URL".equals(node.asText())) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("kind", "INVALID_ENUM", "Kind must be URL")));
        }
    }

    private static String requiredText(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError(field, "REQUIRED", "A value is required")));
        }
        if (!node.isTextual()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError(field, "INVALID_TYPE", "A text value is required")));
        }
        if (node.asText().isBlank()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError(field, "INVALID_VALUE", "A value is required")));
        }
        return node.asText();
    }

    private static <E extends Enum<E>> E enumValue(
            JsonNode node, String field, Class<E> type, List<ExamDraftFieldError> errors) {
        if (node == null || node.isNull()) {
            errors.add(new ExamDraftFieldError(field, "REQUIRED", "A value is required"));
            return null;
        }
        if (!node.isTextual()) {
            errors.add(new ExamDraftFieldError(field, "INVALID_TYPE", "A known value is required"));
            return null;
        }
        try {
            return Enum.valueOf(type, node.asText());
        } catch (IllegalArgumentException ex) {
            errors.add(new ExamDraftFieldError(field, "INVALID_ENUM", "Use a known value"));
            return null;
        }
    }

    private JsonNode object(byte[] raw, String field) {
        if (raw.length == 0) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError(field, "REQUIRED", "A JSON object is required")));
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node == null || !node.isObject()) {
                throw ExamDraftException.validation(
                        "Invalid request",
                        List.of(new ExamDraftFieldError(field, "INVALID_TYPE", "A JSON object is required")));
            }
            return node;
        } catch (IOException ex) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError(field, "INVALID_TYPE", "JSON could not be read")));
        }
    }

    private static void rejectUnknown(JsonNode node, Set<String> allowed, String field) {
        if (unknown(node, allowed)) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError(field, "INVALID_VALUE", "Unsupported property")));
        }
    }

    private static boolean unknown(JsonNode node, Set<String> allowed) {
        var names = node.fieldNames();
        while (names.hasNext()) {
            if (!allowed.contains(names.next())) {
                return true;
            }
        }
        return false;
    }

    private static boolean canonicalInteger(JsonNode node) {
        if (!node.isNumber()) {
            return false;
        }
        JsonParser.NumberType type = node.numberType();
        return type == JsonParser.NumberType.INT
                || type == JsonParser.NumberType.LONG
                || type == JsonParser.NumberType.BIG_INTEGER;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            return null;
        }
        return node.asText();
    }

    private static void rejectQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("query", "INVALID_VALUE", "Query parameters are not supported")));
        }
    }

    private static boolean isMultipartForm(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data");
    }

    private static boolean isJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        String type = contentType.toLowerCase(Locale.ROOT);
        return type.startsWith("application/json");
    }

    /**
     * Security and the multipart resolver wrap the request. Servlet parts are the
     * container view, including a manifest without a file name. MockMvc {@code file()}
     * keeps the bytes on the nested multipart request, so the walk checks that map too.
     */
    private static List<Uploaded> parts(HttpServletRequest request) {
        ServletException failure = null;
        HttpServletRequest current = request;
        Set<HttpServletRequest> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (current != null && seen.add(current)) {
            try {
                Collection<Part> servletParts = current.getParts();
                if (servletParts != null && !servletParts.isEmpty()) {
                    return fromParts(servletParts);
                }
            } catch (IOException | ServletException ex) {
                failure = ex instanceof ServletException servletException
                        ? servletException
                        : new ServletException(ex);
            }
            if (current instanceof MultipartHttpServletRequest multipart && !multipart.getFileMap().isEmpty()) {
                return files(multipart);
            }
            if (current instanceof HttpServletRequestWrapper wrapper
                    && wrapper.getRequest() instanceof HttpServletRequest nested) {
                current = nested;
            } else {
                break;
            }
        }
        if (failure != null) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("manifest", "INVALID_TYPE", "Multipart body could not be read")));
        }
        return List.of();
    }

    private static List<Uploaded> files(MultipartHttpServletRequest multipart) {
        List<Uploaded> uploaded = new ArrayList<>();
        multipart.getMultiFileMap().forEach((name, files) -> {
            for (MultipartFile file : files) {
                uploaded.add(readFile(file));
            }
        });
        return uploaded;
    }

    private static List<Uploaded> fromParts(Collection<Part> parts) {
        List<Uploaded> uploaded = new ArrayList<>();
        for (Part part : parts) {
            uploaded.add(new Uploaded(
                    part.getName(), part.getSubmittedFileName(), part.getContentType(), readBytes(part)));
        }
        return uploaded;
    }

    private static Uploaded readFile(MultipartFile file) {
        try {
            return new Uploaded(file.getName(), file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException ex) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("body", "INVALID_TYPE", "A part could not be read")));
        }
    }

    private static Uploaded sole(List<Uploaded> parts, String name) {
        Uploaded found = null;
        for (Uploaded part : parts) {
            if (name.equals(part.name())) {
                if (found != null) {
                    return null;
                }
                found = part;
            }
        }
        return found;
    }

    private static byte[] readBytes(Part part) {
        try {
            return part.getInputStream().readAllBytes();
        } catch (IOException ex) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("body", "INVALID_TYPE", "A part could not be read")));
        }
    }

    private record Uploaded(String name, String filename, String contentType, byte[] bytes) {
    }

    private static byte[] readBody(HttpServletRequest request) {
        try {
            return request.getInputStream().readAllBytes();
        } catch (IOException ex) {
            throw ExamDraftException.validation(
                    "Invalid request",
                    List.of(new ExamDraftFieldError("body", "INVALID_TYPE", "Request body could not be read")));
        }
    }
}

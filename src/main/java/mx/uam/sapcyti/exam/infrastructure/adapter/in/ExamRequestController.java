package mx.uam.sapcyti.exam.infrastructure.adapter.in;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import mx.uam.sapcyti.exam.application.exception.ExamContentException;
import mx.uam.sapcyti.exam.application.exception.ExamContentFieldError;
import mx.uam.sapcyti.exam.application.exception.ExamContentStreamAbortedException;
import mx.uam.sapcyti.exam.application.exception.ExamDraftException;
import mx.uam.sapcyti.exam.application.exception.ExamDraftFieldError;
import mx.uam.sapcyti.exam.application.exception.ExamReadException;
import mx.uam.sapcyti.exam.application.exception.ExamRequestFieldError;
import mx.uam.sapcyti.exam.application.model.DraftCommands;
import mx.uam.sapcyti.exam.application.model.DraftOutcomes;
import mx.uam.sapcyti.exam.application.model.ExamContentPayload;
import mx.uam.sapcyti.exam.application.service.ContentDispositionValue;
import mx.uam.sapcyti.exam.application.service.ExamDraftUseCase;
import mx.uam.sapcyti.exam.application.service.ReadExamDocumentContentUseCase;
import mx.uam.sapcyti.exam.application.service.ReadExamRequestUseCase;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamOptions;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.infrastructure.adapter.in.dto.DraftMutationResponse;
import mx.uam.sapcyti.exam.infrastructure.adapter.in.dto.ExamDraftResponses;
import mx.uam.sapcyti.exam.infrastructure.adapter.in.dto.ExamOptionsResponse;
import mx.uam.sapcyti.exam.infrastructure.adapter.in.dto.ExamPanelResponse;
import mx.uam.sapcyti.exam.infrastructure.adapter.in.dto.ExamRequirementsResponse;
import mx.uam.sapcyti.exam.infrastructure.adapter.in.dto.SaveDocumentsResponse;
import mx.uam.sapcyti.identity.application.model.AuthenticatedUser;
import mx.uam.sapcyti.identity.infrastructure.security.AuthenticatedUserResolver;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Student exam reads (SPEC-047, SPEC-049), draft writes (SPEC-052) and the current PDF (SPEC-053).
 * Role is also enforced in {@code SecurityConfig} before this controller binds the body.
 * The path exam type is forwarded as parsed. {@code PREDOCTORAL} is not rewritten to {@code GRADO}.
 */
@RestController
@RequestMapping(path = "/api/exam-requests", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExamRequestController {

    private final ReadExamRequestUseCase reads;
    private final ReadExamDocumentContentUseCase contentReads;
    private final ExamDraftUseCase drafts;
    private final ExamDraftHttpReader draftRequests;
    private final AuthenticatedUserResolver users;

    public ExamRequestController(
            ReadExamRequestUseCase reads,
            ReadExamDocumentContentUseCase contentReads,
            ExamDraftUseCase drafts,
            ExamDraftHttpReader draftRequests,
            AuthenticatedUserResolver users) {
        this.reads = reads;
        this.contentReads = contentReads;
        this.drafts = drafts;
        this.draftRequests = draftRequests;
        this.users = users;
    }

    @GetMapping("/me/options")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamOptionsResponse> options(HttpServletRequest request) {
        rejectSelection(request);
        AuthenticatedUser user = users.resolve();
        ExamOptions options = reads.options(user.getUserId(), user.getRole(), user.getGraduateProgramId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ExamOptionsResponse.from(options));
    }

    @GetMapping("/me/{examType}/requirements")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamRequirementsResponse> requirements(
            @PathVariable String examType, HttpServletRequest request) {
        ExamType type = parseExamType(examType);
        rejectSelection(request);
        AuthenticatedUser user = users.resolve();
        ExamRequirements requirements = reads.requirements(
                user.getUserId(), user.getRole(), user.getGraduateProgramId(), type);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ExamRequirementsResponse.from(requirements));
    }

    @PostMapping("/me/{examType}/documents")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SaveDocumentsResponse> saveDocuments(
            @PathVariable String examType, HttpServletRequest request) {
        ExamType type = parseDraftExamType(examType);
        AuthenticatedUser user = users.resolve();
        DraftCommands.Batch batch = draftRequests.readBatch(request);
        DraftOutcomes.Save saved = drafts.save(
                user.getUserId(), user.getRole(), user.getGraduateProgramId(), type, batch);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ExamDraftResponses.save(saved));
    }

    @PutMapping("/me/{examType}/documents/{documentType}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DraftMutationResponse> replaceDocument(
            @PathVariable String examType, @PathVariable String documentType, HttpServletRequest request) {
        ExamType type = parseDraftExamType(examType);
        DocumentType document = parseDocumentType(documentType);
        AuthenticatedUser user = users.resolve();
        DraftOutcomes.Mutation mutation = replace(user, type, document, request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ExamDraftResponses.mutation(mutation));
    }

    @DeleteMapping("/me/{examType}/documents/{documentType}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DraftMutationResponse> deleteDocument(
            @PathVariable String examType, @PathVariable String documentType, HttpServletRequest request) {
        ExamType type = parseDraftExamType(examType);
        DocumentType document = parseDocumentType(documentType);
        AuthenticatedUser user = users.resolve();
        int version = draftRequests.readDeleteVersion(request);
        DraftOutcomes.Mutation mutation = drafts.delete(
                user.getUserId(), user.getRole(), user.getGraduateProgramId(), type, document, version);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ExamDraftResponses.mutation(mutation));
    }

    @GetMapping("/me/{examType}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamPanelResponse> panel(@PathVariable String examType, HttpServletRequest request) {
        ExamType type = parseDraftExamType(examType);
        rejectDraftSelection(request);
        AuthenticatedUser user = users.resolve();
        DraftOutcomes.Panel panel = drafts.panel(
                user.getUserId(), user.getRole(), user.getGraduateProgramId(), type);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ExamDraftResponses.panel(panel));
    }

    @GetMapping(path = "/me/{examType}/documents/{documentType}/content", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public void content(
            @PathVariable String examType,
            @PathVariable String documentType,
            HttpServletRequest request,
            HttpServletResponse response) {
        ExamType type = parseContentExamType(examType);
        DocumentType document = parseContentDocumentType(documentType);
        String disposition = readDisposition(request);
        rejectContentBody(request);
        rejectExtraQuery(request);
        AuthenticatedUser user = users.resolve();
        ExamContentPayload payload = contentReads.read(
                user.getUserId(),
                user.getRole(),
                user.getGraduateProgramId(),
                type,
                document,
                disposition);
        writePdf(response, payload);
    }

    private DraftOutcomes.Mutation replace(
            AuthenticatedUser user, ExamType examType, DocumentType documentType, HttpServletRequest request) {
        if (draftRequestsIsJson(request)) {
            DraftCommands.UrlReplacement command = draftRequests.readUrlReplacement(request);
            return drafts.replaceUrl(
                    user.getUserId(), user.getRole(), user.getGraduateProgramId(), examType, documentType, command);
        }
        DraftCommands.PdfReplacement command = draftRequests.readPdfReplacement(request);
        return drafts.replacePdf(
                user.getUserId(), user.getRole(), user.getGraduateProgramId(), examType, documentType, command);
    }

    private static boolean draftRequestsIsJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json");
    }

    private static ExamType parseDraftExamType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ExamDraftException.validation("Invalid exam type", List.of(new ExamDraftFieldError(
                    "examType", "REQUIRED", "Exam type is required")));
        }
        try {
            return ExamType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw ExamDraftException.validation("Invalid exam type", List.of(new ExamDraftFieldError(
                    "examType", "INVALID_ENUM", "Use GRADO or PREDOCTORAL")));
        }
    }

    private static DocumentType parseDocumentType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ExamDraftException.validation("Invalid document type", List.of(new ExamDraftFieldError(
                    "documentType", "REQUIRED", "Document type is required")));
        }
        try {
            return DocumentType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw ExamDraftException.validation("Invalid document type", List.of(new ExamDraftFieldError(
                    "documentType", "INVALID_ENUM", "Use a known document type")));
        }
    }

    private static void rejectDraftSelection(HttpServletRequest request) {
        List<ExamDraftFieldError> errors = new ArrayList<>();
        if (hasQuery(request)) {
            errors.add(new ExamDraftFieldError("query", "INVALID_VALUE", "Query parameters are not supported"));
        }
        if (!isBlank(readBody(request))) {
            errors.add(new ExamDraftFieldError("body", "INVALID_VALUE", "Request body is not supported"));
        }
        if (!errors.isEmpty()) {
            throw ExamDraftException.validation("Invalid request", errors);
        }
    }

    private static ExamType parseExamType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ExamReadException.validation("Invalid exam type", List.of(new ExamRequestFieldError(
                    "examType", "REQUIRED", "Exam type is required")));
        }
        try {
            return ExamType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw ExamReadException.validation("Invalid exam type", List.of(new ExamRequestFieldError(
                    "examType", "INVALID_ENUM", "Use GRADO or PREDOCTORAL")));
        }
    }

    private static void rejectSelection(HttpServletRequest request) {
        List<ExamRequestFieldError> errors = new ArrayList<>();
        if (hasQuery(request)) {
            errors.add(new ExamRequestFieldError(
                    "query", "INVALID_VALUE", "Query parameters are not supported"));
        }
        if (!isBlank(readBody(request))) {
            errors.add(new ExamRequestFieldError(
                    "body", "INVALID_VALUE", "Request body is not supported"));
        }
        if (!errors.isEmpty()) {
            throw ExamReadException.validation("Invalid request", errors);
        }
    }

    private static boolean hasQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query != null && !query.isBlank();
    }

    private static byte[] readBody(HttpServletRequest request) {
        try {
            return request.getInputStream().readAllBytes();
        } catch (IOException ex) {
            throw ExamReadException.validation("Invalid request", List.of(new ExamRequestFieldError(
                    "body", "INVALID_TYPE", "Request body could not be read")));
        }
    }

    private static boolean isBlank(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return true;
        }
        for (byte value : raw) {
            if (value != ' ' && value != '\n' && value != '\r' && value != '\t') {
                return false;
            }
        }
        return true;
    }

    private static ExamType parseContentExamType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ExamContentException.validation("Invalid exam type", List.of(new ExamContentFieldError(
                    "examType", "REQUIRED", "Exam type is required")));
        }
        try {
            return ExamType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw ExamContentException.validation("Invalid exam type", List.of(new ExamContentFieldError(
                    "examType", "INVALID_ENUM", "Use GRADO or PREDOCTORAL")));
        }
    }

    private static DocumentType parseContentDocumentType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ExamContentException.validation("Invalid document type", List.of(new ExamContentFieldError(
                    "documentType", "REQUIRED", "Document type is required")));
        }
        try {
            return DocumentType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw ExamContentException.validation("Invalid document type", List.of(new ExamContentFieldError(
                    "documentType", "INVALID_ENUM", "Use a known document type")));
        }
    }

    private static String readDisposition(HttpServletRequest request) {
        String[] values = request.getParameterValues("disposition");
        if (values == null) {
            return "inline";
        }
        if (values.length == 1 && ("inline".equals(values[0]) || "attachment".equals(values[0]))) {
            return values[0];
        }
        throw ExamContentException.validation("Invalid disposition", List.of(new ExamContentFieldError(
                "disposition", "INVALID_ENUM", "Use inline or attachment")));
    }

    private static void rejectContentBody(HttpServletRequest request) {
        if (!isBlank(readContentBody(request))) {
            throw ExamContentException.validation("Invalid request", List.of(new ExamContentFieldError(
                    "body", "INVALID_VALUE", "Request body is not supported")));
        }
    }

    private static byte[] readContentBody(HttpServletRequest request) {
        try {
            return request.getInputStream().readAllBytes();
        } catch (IOException ex) {
            throw ExamContentException.validation("Invalid request", List.of(new ExamContentFieldError(
                    "body", "INVALID_TYPE", "Request body could not be read")));
        }
    }

    private static void rejectExtraQuery(HttpServletRequest request) {
        for (String name : request.getParameterMap().keySet()) {
            if (!"disposition".equals(name)) {
                throw ExamContentException.validation("Invalid request", List.of(new ExamContentFieldError(
                        "query", "INVALID_VALUE", "Query parameters are not supported")));
            }
        }
    }

    private static void writePdf(HttpServletResponse response, ExamContentPayload payload) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Type", MediaType.APPLICATION_PDF_VALUE);
        response.setHeader("Content-Disposition", ContentDispositionValue.format(
                payload.disposition(), payload.originalName()));
        response.setHeader("Accept-Ranges", "none");
        response.setHeader("Cache-Control", CacheControl.noStore().getHeaderValue());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setContentLengthLong(payload.object().sizeBytes());
        InputStream body = payload.object().body();
        try {
            response.flushBuffer();
            body.transferTo(response.getOutputStream());
        } catch (IOException | RuntimeException ex) {
            if (response.isCommitted()) {
                throw new ExamContentStreamAbortedException();
            }
            throw ExamContentException.storageUnavailable();
        } finally {
            try {
                body.close();
            } catch (IOException ex) {
                // Headers are already fixed. Closing the object does not add a JSON body.
            }
        }
    }
}

package mx.uam.sapcyti.exam.infrastructure.adapter.in;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import mx.uam.sapcyti.exam.application.exception.ExamReadException;
import mx.uam.sapcyti.exam.application.exception.ExamRequestFieldError;
import mx.uam.sapcyti.exam.application.service.ReadExamRequestUseCase;
import mx.uam.sapcyti.exam.domain.model.ExamOptions;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.infrastructure.adapter.in.dto.ExamOptionsResponse;
import mx.uam.sapcyti.exam.infrastructure.adapter.in.dto.ExamRequirementsResponse;
import mx.uam.sapcyti.identity.application.model.AuthenticatedUser;
import mx.uam.sapcyti.identity.infrastructure.security.AuthenticatedUserResolver;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Student reads of exam options and degree requirements (SPEC-047).
 * Role is also enforced in {@code SecurityConfig} before this controller binds the body.
 */
@RestController
@RequestMapping(path = "/api/exam-requests", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExamRequestController {

    private final ReadExamRequestUseCase reads;
    private final AuthenticatedUserResolver users;

    public ExamRequestController(ReadExamRequestUseCase reads, AuthenticatedUserResolver users) {
        this.reads = reads;
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
}

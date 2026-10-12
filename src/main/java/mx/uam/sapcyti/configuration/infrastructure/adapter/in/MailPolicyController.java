package mx.uam.sapcyti.configuration.infrastructure.adapter.in;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import mx.uam.sapcyti.configuration.domain.exception.MailPolicyFieldError;
import mx.uam.sapcyti.configuration.domain.exception.MailPolicyUnsupportedMediaTypeException;
import mx.uam.sapcyti.configuration.domain.exception.MailPolicyValidationException;
import mx.uam.sapcyti.configuration.domain.model.MailPolicy;
import mx.uam.sapcyti.configuration.domain.model.UpdateMailPolicyCommand;
import mx.uam.sapcyti.configuration.domain.port.in.GetMailPolicyInputPort;
import mx.uam.sapcyti.configuration.domain.port.in.UpdateMailPolicyInputPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.MailPolicyResponse;

/**
 * REST API for the global mail policy (SPEC-041).
 * Role is also enforced in {@code SecurityConfig} before this controller binds the body.
 */
@RestController
@RequestMapping(path = "/api/mail-policy", produces = MediaType.APPLICATION_JSON_VALUE)
public class MailPolicyController {

    private final GetMailPolicyInputPort getPort;
    private final UpdateMailPolicyInputPort updatePort;

    public MailPolicyController(
            GetMailPolicyInputPort getPort,
            UpdateMailPolicyInputPort updatePort) {
        this.getPort = getPort;
        this.updatePort = updatePort;
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<MailPolicyResponse> get(HttpServletRequest request) {
        rejectSelection(request);
        return ok(getPort.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<MailPolicyResponse> put(HttpServletRequest request) {
        rejectQuery(request);
        requireJson(request);
        UpdateMailPolicyCommand command = MailPolicyRequestReader.parse(readBody(request));
        return ok(updatePort.update(command));
    }

    private static ResponseEntity<MailPolicyResponse> ok(MailPolicy policy) {
        MailPolicyResponse body = new MailPolicyResponse(
                policy.getMode(),
                policy.getVersion(),
                policy.getUpdatedAt(),
                policy.getUpdatedByUserId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    private static void rejectSelection(HttpServletRequest request) {
        List<MailPolicyFieldError> errors = new ArrayList<>();
        if (hasQuery(request)) {
            errors.add(new MailPolicyFieldError(
                    "query", "INVALID_VALUE", "Query parameters are not supported"));
        }
        if (!isBlank(readBody(request))) {
            errors.add(new MailPolicyFieldError(
                    "body", "INVALID_VALUE", "Request body is not supported"));
        }
        if (!errors.isEmpty()) {
            throw new MailPolicyValidationException(errors);
        }
    }

    private static void rejectQuery(HttpServletRequest request) {
        if (hasQuery(request)) {
            throw new MailPolicyValidationException(List.of(new MailPolicyFieldError(
                    "query", "INVALID_VALUE", "Query parameters are not supported")));
        }
    }

    private static boolean hasQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query != null && !query.isBlank();
    }

    private static void requireJson(HttpServletRequest request) {
        if (!isApplicationJson(request.getContentType())) {
            throw new MailPolicyUnsupportedMediaTypeException();
        }
    }

    private static boolean isApplicationJson(String header) {
        if (header == null || header.isBlank()) {
            return false;
        }
        try {
            MediaType parsed = MediaType.parseMediaType(header);
            return "application".equalsIgnoreCase(parsed.getType())
                    && "json".equalsIgnoreCase(parsed.getSubtype());
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }

    private static byte[] readBody(HttpServletRequest request) {
        try {
            return request.getInputStream().readAllBytes();
        } catch (IOException ex) {
            throw new MailPolicyValidationException(List.of(new MailPolicyFieldError(
                    "body", "INVALID_TYPE", "Request body must be a JSON object")));
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

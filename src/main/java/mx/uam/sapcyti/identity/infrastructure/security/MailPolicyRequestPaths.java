package mx.uam.sapcyti.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;

/**
 * Path check for responses written before the controller.
 * SPEC-041 is the mail policy. SPEC-047 is the two student exam reads.
 * SPEC-052 is the draft panel and its writes. SPEC-053 is the current PDF.
 */
final class MailPolicyRequestPaths {

    private static final String EXAM_OPTIONS = "/api/exam-requests/me/options";
    private static final String EXAM_REQUIREMENTS_PREFIX = "/api/exam-requests/me/";
    private static final String EXAM_REQUIREMENTS_SUFFIX = "/requirements";

    private MailPolicyRequestPaths() {
    }

    static boolean isMailPolicy(HttpServletRequest request) {
        return "/api/mail-policy".equals(path(request));
    }

    static void noStore(HttpServletRequest request, HttpServletResponse response) {
        if (isExamContent(request)) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Referrer-Policy", "no-referrer");
            return;
        }
        if (isMailPolicy(request) || isExamRead(request) || isExamDraft(request)) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        }
    }

    private static boolean isExamContent(HttpServletRequest request) {
        String path = path(request);
        String prefix = EXAM_REQUIREMENTS_PREFIX;
        if (!path.startsWith(prefix) || !path.endsWith("/content")) {
            return false;
        }
        String rest = path.substring(prefix.length(), path.length() - "/content".length());
        String[] parts = rest.split("/");
        return parts.length == 3
                && !parts[0].isEmpty()
                && "documents".equals(parts[1])
                && !parts[2].isEmpty();
    }

    private static boolean isExamDraft(HttpServletRequest request) {
        String path = path(request);
        String prefix = EXAM_REQUIREMENTS_PREFIX;
        if (!path.startsWith(prefix)) {
            return false;
        }
        String rest = path.substring(prefix.length());
        if (rest.isEmpty() || "options".equals(rest) || rest.endsWith(EXAM_REQUIREMENTS_SUFFIX)) {
            return false;
        }
        String[] parts = rest.split("/");
        if (parts.length == 1) {
            return true;
        }
        if (parts.length == 2) {
            return "documents".equals(parts[1]);
        }
        return parts.length == 3 && "documents".equals(parts[1]) && !parts[2].isEmpty();
    }

    private static boolean isExamRead(HttpServletRequest request) {
        String path = path(request);
        if (EXAM_OPTIONS.equals(path)) {
            return true;
        }
        if (!path.startsWith(EXAM_REQUIREMENTS_PREFIX) || !path.endsWith(EXAM_REQUIREMENTS_SUFFIX)) {
            return false;
        }
        String examType = path.substring(
                EXAM_REQUIREMENTS_PREFIX.length(),
                path.length() - EXAM_REQUIREMENTS_SUFFIX.length());
        return !examType.isEmpty() && examType.indexOf('/') < 0;
    }

    private static String path(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path != null && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        return path == null ? "" : path;
    }
}

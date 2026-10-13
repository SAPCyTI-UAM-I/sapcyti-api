package mx.uam.sapcyti.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;

/**
 * Path check for responses written before the controller.
 * SPEC-041 is the mail policy. SPEC-047 is the two student exam reads.
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
        if (isMailPolicy(request) || isExamRead(request)) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        }
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

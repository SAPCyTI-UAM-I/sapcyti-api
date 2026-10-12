package mx.uam.sapcyti.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;

/**
 * Path check for SPEC-041 responses that are written before the controller.
 */
final class MailPolicyRequestPaths {

    private MailPolicyRequestPaths() {
    }

    static boolean isMailPolicy(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path != null && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        return "/api/mail-policy".equals(path);
    }

    static void noStore(HttpServletRequest request, HttpServletResponse response) {
        if (isMailPolicy(request)) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        }
    }
}

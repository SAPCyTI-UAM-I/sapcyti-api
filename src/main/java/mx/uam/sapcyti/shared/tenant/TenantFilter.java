package mx.uam.sapcyti.shared.tenant;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(0)
public class TenantFilter extends OncePerRequestFilter {

    public static final String HEADER_GRADUATE_ID = "X-Graduate-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    public static final String MDC_GRADUATE_PROGRAM_ID = "graduate_program_id";
    public static final String MDC_USER_ID = "user_id";
    public static final String MDC_REQUEST_ID = "request_id";

    private static final Logger LOG = LoggerFactory.getLogger(TenantFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String graduateHeader = request.getHeader(HEADER_GRADUATE_ID);
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            response.setHeader(HEADER_REQUEST_ID, requestId);
            MDC.put(MDC_REQUEST_ID, requestId);

            if (graduateHeader != null && !graduateHeader.isBlank()) {
                try {
                    long id = Long.parseLong(graduateHeader.trim());
                    TenantContext.set(id);
                    MDC.put(MDC_GRADUATE_PROGRAM_ID, String.valueOf(id));
                } catch (NumberFormatException ex) {
                    LOG.warn("Invalid {} header value: {}", HEADER_GRADUATE_ID, graduateHeader);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove(MDC_GRADUATE_PROGRAM_ID);
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
        }
    }
}

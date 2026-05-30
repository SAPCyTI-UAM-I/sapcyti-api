package mx.uam.sapcyti.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.application.service.JwtService;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContextHelper;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import mx.uam.sapcyti.shared.tenant.TenantResolver;
import mx.uam.sapcyti.shared.web.ErrorResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final Claims claims = jwtService.validateToken(jwt);

            final String userId = claims.getSubject();
            final String role = claims.get("role", String.class);
            final Long claimProgramId = readGraduateProgramId(claims);
            final String rawGraduateHeader = request.getHeader(TenantFilter.HEADER_GRADUATE_ID);

            Optional<Long> headerProgramId;
            if (TenantResolver.isHeaderPresentButInvalid(rawGraduateHeader)) {
                if (RoleType.SYSTEM_ADMIN.name().equals(role)) {
                    headerProgramId = Optional.empty();
                } else {
                    writeForbidden(response, TenantAccessDeniedException.INVALID_HEADER_MESSAGE);
                    return;
                }
            } else {
                headerProgramId = TenantResolver.parseGraduateHeader(rawGraduateHeader);
            }

            Optional<Long> effectiveProgramId =
                    TenantResolver.resolve(role, claimProgramId, headerProgramId);
            TenantContextHelper.apply(effectiveProgramId, userId);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);
        } catch (TenantAccessDeniedException ex) {
            writeForbidden(response, ex.getMessage());
        } catch (Exception e) {
            logger.error("Could not set user authentication in security context", e);
            filterChain.doFilter(request, response);
        }
    }

    private static Long readGraduateProgramId(Claims claims) {
        Object value = claims.get("graduateProgramId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse("FORBIDDEN", message));
    }
}

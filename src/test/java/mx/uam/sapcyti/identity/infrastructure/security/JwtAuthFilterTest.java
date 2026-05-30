package mx.uam.sapcyti.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import mx.uam.sapcyti.identity.application.service.JwtService;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, new ObjectMapper());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("should set authentication and tenant from JWT claim")
    void shouldSetAuthContextAndTenantFromClaim() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");

        DefaultClaims claims = new DefaultClaims(Map.of(
                "sub", "123",
                "role", "STUDENT",
                "graduateProgramId", 7));
        when(jwtService.validateToken("valid-token")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("123");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_STUDENT");
        assertThat(TenantContext.get()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("should return 403 when header does not match claim")
    void shouldRejectMismatchedHeader() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        request.addHeader(TenantFilter.HEADER_GRADUATE_ID, "99");

        DefaultClaims claims = new DefaultClaims(Map.of(
                "sub", "123",
                "role", "COORDINATOR",
                "graduateProgramId", 7));
        when(jwtService.validateToken("valid-token")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("FORBIDDEN");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    @DisplayName("SYSTEM_ADMIN may override tenant via header")
    void systemAdminHeaderOverride() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer admin-token");
        request.addHeader(TenantFilter.HEADER_GRADUATE_ID, "42");

        DefaultClaims claims = new DefaultClaims(Map.of("sub", "1", "role", "SYSTEM_ADMIN"));
        when(jwtService.validateToken("admin-token")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(TenantContext.get()).isEqualTo(42L);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("should ignore missing Authorization header")
    void shouldIgnoreMissingHeader() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    @DisplayName("should handle invalid token gracefully")
    void shouldHandleInvalidTokenGracefully() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtService.validateToken("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isNull();
    }
}

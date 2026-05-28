package mx.uam.sapcyti.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import mx.uam.sapcyti.identity.application.service.JwtService;
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
        filter = new JwtAuthFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = spy(new MockFilterChain());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should set authentication context when token is valid")
    void shouldSetAuthContextWhenTokenValid() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");

        DefaultClaims claims = new DefaultClaims(Map.of("sub", "123", "role", "STUDENT"));
        when(jwtService.validateToken("valid-token")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("123");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_STUDENT");
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should ignore missing Authorization header")
    void shouldIgnoreMissingHeader() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should handle invalid token gracefully")
    void shouldHandleInvalidTokenGracefully() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtService.validateToken("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}

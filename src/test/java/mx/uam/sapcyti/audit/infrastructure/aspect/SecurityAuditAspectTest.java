package mx.uam.sapcyti.audit.infrastructure.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.port.in.AuthInputPort;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class SecurityAuditAspectTest {

    @Mock
    private AuditOutputPort auditOutputPort;

    @InjectMocks
    private SecurityAuditAspect securityAuditAspect;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("should record successful login")
    void shouldRecordSuccessfulLogin() {
        AuthInputPort.LoginResult result = AuthInputPort.LoginResult.builder()
                .userId(1L)
                .graduateProgramId(2L)
                .authResponse(AuthResponse.builder().role(RoleType.STUDENT).build())
                .build();

        securityAuditAspect.auditLoginSuccess(result);

        verify(auditOutputPort).record(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        
        assertThat(event.getAction()).isEqualTo("LOGIN_SUCCESS");
        assertThat(event.getActorId()).isEqualTo(1L);
        assertThat(event.getActorRole()).isEqualTo("STUDENT");
        assertThat(event.getGraduateProgramId()).isEqualTo(2L);
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("should ignore non-LoginResult returns on success")
    void shouldIgnoreNonLoginResultReturns() {
        securityAuditAspect.auditLoginSuccess("some-other-result");
        // Verify no interactions
        org.mockito.Mockito.verifyNoInteractions(auditOutputPort);
    }

    @Test
    @DisplayName("should record failed login")
    void shouldRecordFailedLogin() {
        Exception ex = new RuntimeException("Bad credentials");

        securityAuditAspect.auditLoginFailure(ex);

        verify(auditOutputPort).record(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        
        assertThat(event.getAction()).isEqualTo("LOGIN_FAILED");
        assertThat(event.getActorId()).isNull();
        assertThat(event.getActorRole()).isEqualTo("ANONYMOUS");
        assertThat(event.getDetails()).isEqualTo("Bad credentials");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("should handle missing request context gracefully")
    void shouldHandleMissingRequestContextGracefully() {
        RequestContextHolder.resetRequestAttributes();
        
        Exception ex = new RuntimeException("Bad credentials");
        securityAuditAspect.auditLoginFailure(ex);

        verify(auditOutputPort).record(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        
        assertThat(event.getIpAddress()).isEqualTo("unknown");
    }
}

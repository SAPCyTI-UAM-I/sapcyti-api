package mx.uam.sapcyti.audit.infrastructure.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.identity.application.service.ChangePasswordUseCase;
import mx.uam.sapcyti.identity.application.service.ForgotPasswordUseCase;
import mx.uam.sapcyti.identity.application.service.ResetPasswordUseCase;
import mx.uam.sapcyti.identity.domain.exception.PasswordChangeForbiddenException;
import mx.uam.sapcyti.identity.domain.model.RoleType;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class PasswordAuditAspectTest {

    @Mock
    private AuditOutputPort auditOutputPort;

    @InjectMocks
    private PasswordAuditAspect passwordAuditAspect;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("records PASSWORD_CHANGED with changeType SELF")
    void auditSelfPasswordChange() {
        ChangePasswordUseCase.ChangePasswordResult result = ChangePasswordUseCase.ChangePasswordResult.builder()
                .userId(50L)
                .role(RoleType.STUDENT.name())
                .graduateProgramId(10L)
                .changeType("SELF")
                .build();

        passwordAuditAspect.auditPasswordChangedByUser(result);

        verify(auditOutputPort).record(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo("PASSWORD_CHANGED");
        assertThat(event.getActorId()).isEqualTo(50L);
        assertThat(event.getDetails()).isEqualTo("changeType=SELF");
        assertThat(event.getIpAddress()).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("records PASSWORD_CHANGED with changeType COORDINATOR")
    void auditCoordinatorPasswordChange() {
        ChangePasswordUseCase.ChangePasswordResult result = ChangePasswordUseCase.ChangePasswordResult.builder()
                .userId(50L)
                .role(RoleType.STUDENT.name())
                .graduateProgramId(10L)
                .changeType("COORDINATOR")
                .build();

        passwordAuditAspect.auditPasswordChangedByUser(result);

        verify(auditOutputPort).record(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDetails()).isEqualTo("changeType=COORDINATOR");
    }

    @Test
    @DisplayName("records PASSWORD_CHANGED with changeType RESET for reset flow")
    void auditResetPasswordChange() {
        ResetPasswordUseCase.ResetPasswordResult result = ResetPasswordUseCase.ResetPasswordResult.builder()
                .userId(5L)
                .role(RoleType.STUDENT.name())
                .graduateProgramId(10L)
                .build();

        passwordAuditAspect.auditPasswordResetCompleted(result);

        verify(auditOutputPort).record(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDetails()).isEqualTo("changeType=RESET");
    }

    @Test
    @DisplayName("records RBAC_VIOLATION_DETECTED on forbidden password change")
    void auditRbacViolation() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "50",
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        passwordAuditAspect.auditPasswordChangeRbacViolation(new PasswordChangeForbiddenException());

        verify(auditOutputPort).record(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo("RBAC_VIOLATION_DETECTED");
        assertThat(event.getActorId()).isEqualTo(50L);
        assertThat(event.getActorRole()).isEqualTo("STUDENT");
        assertThat(event.getDetails()).isEqualTo(PasswordChangeForbiddenException.MESSAGE);
    }

    @Test
    @DisplayName("ignores non-change-password results")
    void ignoresUnknownResults() {
        passwordAuditAspect.auditPasswordChangedByUser("other");
        verifyNoInteractions(auditOutputPort);
    }

    @Test
    @DisplayName("records forgot-password request when reset was requested")
    void auditForgotPasswordRequested() {
        ForgotPasswordUseCase.ForgotPasswordResult result = ForgotPasswordUseCase.ForgotPasswordResult.builder()
                .resetRequested(true)
                .userId(7L)
                .role(RoleType.STUDENT.name())
                .graduateProgramId(3L)
                .build();

        passwordAuditAspect.auditPasswordResetRequested(result);

        verify(auditOutputPort).record(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAction()).isEqualTo("PASSWORD_RESET_REQUESTED");
    }
}

package mx.uam.sapcyti.audit.infrastructure.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.model.AuditSeverity;
import mx.uam.sapcyti.audit.domain.model.AuditSeverityLevel;
import mx.uam.sapcyti.audit.domain.model.KnownAuditActions;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.identity.domain.port.in.AuthInputPort;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.AuthResponse;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditAspect {

    private final AuditOutputPort auditOutputPort;

    @Pointcut("execution(* mx.uam.sapcyti.identity.application.service.AuthService.login(..))")
    public void loginPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.identity.application.service.AuthService.logout(..))")
    public void logoutPointcut() {}

    @AfterReturning(pointcut = "loginPointcut()", returning = "result")
    public void auditLoginSuccess(Object result) {
        if (result instanceof AuthInputPort.LoginResult loginResult) {
            AuthResponse response = loginResult.getAuthResponse();
            
            recordEvent(
                KnownAuditActions.LOGIN_SUCCESS.name(),
                loginResult.getUserId(),
                response.getRole().name(),
                loginResult.getGraduateProgramId(),
                AuditSeverityLevel.HIGH,
                "Successful login"
            );
        }
    }

    @AfterThrowing(pointcut = "loginPointcut()", throwing = "ex")
    public void auditLoginFailure(Exception ex) {
        recordEvent(
            KnownAuditActions.LOGIN_FAILED.name(),
            null,
            "ANONYMOUS",
            null,
            AuditSeverityLevel.HIGH,
            ex.getMessage()
        );
    }

    private void recordEvent(String action, Long actorId, String role, Long programId, AuditSeverityLevel severity, String details) {
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = request != null ? request.getRemoteAddr() : "unknown";

        AuditEvent event = AuditEvent.builder()
                .timestamp(Instant.now())
                .action(action)
                .actorId(actorId)
                .actorRole(role)
                .graduateProgramId(programId)
                .severity(new AuditSeverity(severity))
                .ipAddress(ipAddress)
                .details(details)
                .build();

        auditOutputPort.record(event);
        log.info("AUDIT: {}", event.getAction());
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

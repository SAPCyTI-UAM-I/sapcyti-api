package mx.uam.sapcyti.audit.infrastructure.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.model.AuditSeverity;
import mx.uam.sapcyti.audit.domain.model.AuditSeverityLevel;
import mx.uam.sapcyti.audit.domain.model.KnownAuditActions;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.identity.application.service.ForgotPasswordUseCase;
import mx.uam.sapcyti.identity.application.service.ResetPasswordUseCase;
import org.aspectj.lang.annotation.AfterReturning;
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
public class PasswordAuditAspect {

    private final AuditOutputPort auditOutputPort;

    @Pointcut("execution(* mx.uam.sapcyti.identity.application.service.ForgotPasswordUseCase.execute(..))")
    public void forgotPasswordPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.identity.application.service.ResetPasswordUseCase.execute(..))")
    public void resetPasswordPointcut() {}

    @AfterReturning(pointcut = "forgotPasswordPointcut()", returning = "result")
    public void auditPasswordResetRequested(Object result) {
        if (result instanceof ForgotPasswordUseCase.ForgotPasswordResult forgotResult
                && forgotResult.isResetRequested()) {
            recordEvent(
                    KnownAuditActions.PASSWORD_RESET_REQUESTED.name(),
                    forgotResult.getUserId(),
                    forgotResult.getRole(),
                    forgotResult.getGraduateProgramId(),
                    AuditSeverityLevel.HIGH,
                    "Password reset requested");
        }
    }

    @AfterReturning(pointcut = "resetPasswordPointcut()", returning = "result")
    public void auditPasswordChanged(Object result) {
        if (result instanceof ResetPasswordUseCase.ResetPasswordResult resetResult) {
            recordEvent(
                    KnownAuditActions.PASSWORD_CHANGED.name(),
                    resetResult.getUserId(),
                    resetResult.getRole(),
                    resetResult.getGraduateProgramId(),
                    AuditSeverityLevel.HIGH,
                    "changeType=RESET");
        }
    }

    private void recordEvent(
            String action,
            Long actorId,
            String role,
            Long programId,
            AuditSeverityLevel severity,
            String details) {
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

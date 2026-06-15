package mx.uam.sapcyti.audit.infrastructure.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.model.AuditSeverity;
import mx.uam.sapcyti.audit.domain.model.AuditSeverityLevel;
import mx.uam.sapcyti.audit.domain.model.KnownAuditActions;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.identity.application.service.ChangePasswordUseCase;
import mx.uam.sapcyti.identity.application.service.ForgotPasswordUseCase;
import mx.uam.sapcyti.identity.application.service.ResetPasswordUseCase;
import mx.uam.sapcyti.identity.domain.exception.PasswordChangeForbiddenException;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Pointcut("execution(* mx.uam.sapcyti.identity.application.service.ChangePasswordUseCase.execute(..))")
    public void changePasswordPointcut() {}

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
    public void auditPasswordResetCompleted(Object result) {
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

    @AfterReturning(pointcut = "changePasswordPointcut()", returning = "result")
    public void auditPasswordChangedByUser(Object result) {
        if (result instanceof ChangePasswordUseCase.ChangePasswordResult changeResult) {
            recordEvent(
                    KnownAuditActions.PASSWORD_CHANGED.name(),
                    changeResult.getUserId(),
                    changeResult.getRole(),
                    changeResult.getGraduateProgramId(),
                    AuditSeverityLevel.HIGH,
                    "changeType=" + changeResult.getChangeType());
        }
    }

    @AfterThrowing(pointcut = "changePasswordPointcut()", throwing = "ex")
    public void auditPasswordChangeRbacViolation(PasswordChangeForbiddenException ex) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        Long actorId = Long.parseLong(authentication.getName());
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replace("ROLE_", ""))
                .orElse(RoleType.STUDENT.name());

        recordEvent(
                KnownAuditActions.RBAC_VIOLATION_DETECTED.name(),
                actorId,
                role,
                TenantContext.get(),
                AuditSeverityLevel.HIGH,
                ex.getMessage());
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

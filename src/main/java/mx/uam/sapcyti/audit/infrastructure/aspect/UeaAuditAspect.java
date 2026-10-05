package mx.uam.sapcyti.audit.infrastructure.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.model.AuditSeverity;
import mx.uam.sapcyti.audit.domain.model.AuditSeverityLevel;
import mx.uam.sapcyti.audit.domain.model.KnownAuditActions;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.offering.application.service.BulkUploadUeasUseCase;
import mx.uam.sapcyti.offering.application.service.DeactivateUeaUseCase;
import mx.uam.sapcyti.offering.application.service.RegisterUeaUseCase;
import mx.uam.sapcyti.offering.application.service.RestoreUeaUseCase;
import mx.uam.sapcyti.offering.application.service.UpdateUeaUseCase;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.access.AccessDeniedException;
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
public class UeaAuditAspect {

    private final AuditOutputPort auditOutputPort;

    @Pointcut("execution(* mx.uam.sapcyti.offering.application.service.RegisterUeaUseCase.execute(..))")
    public void registerUeaPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.offering.application.service.BulkUploadUeasUseCase.execute(..))")
    public void bulkUploadUeaPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.offering.application.service.UpdateUeaUseCase.execute(..))")
    public void updateUeaPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.offering.application.service.DeactivateUeaUseCase.execute(..))")
    public void deactivateUeaPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.offering.application.service.RestoreUeaUseCase.execute(..))")
    public void restoreUeaPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.offering.infrastructure.adapter.in.UeaController.*(..))")
    public void ueaControllerPointcut() {}

    @AfterReturning(pointcut = "registerUeaPointcut()", returning = "result")
    public void auditUeaRegistered(Object result) {
        if (!(result instanceof RegisterUeaUseCase.RegisterUeaResult registered)) {
            return;
        }

        recordEvent(
                KnownAuditActions.UEA_REGISTERED.name(),
                resolveActorId(),
                resolveActorRole(),
                registered.getGraduateProgramId(),
                AuditSeverityLevel.STANDARD,
                "ueaId=%d,clave=%s,graduateProgramId=%d".formatted(
                        registered.getId(), registered.getClave(), registered.getGraduateProgramId()));
    }

    @AfterReturning(pointcut = "bulkUploadUeaPointcut()", returning = "result")
    public void auditUeaBulkUploaded(Object result) {
        if (!(result instanceof BulkUploadUeasUseCase.BulkUploadResult uploaded)) {
            return;
        }
        if (uploaded.getCreated() <= 0) {
            return;
        }

        recordEvent(
                KnownAuditActions.UEA_BULK_UPLOADED.name(),
                resolveActorId(),
                resolveActorRole(),
                TenantContext.get(),
                AuditSeverityLevel.STANDARD,
                "graduateProgramId=%d,createdCount=%d".formatted(
                        TenantContext.get(), uploaded.getCreated()));
    }

    @AfterReturning(pointcut = "updateUeaPointcut()", returning = "result")
    public void auditUeaUpdated(Object result) {
        if (!(result instanceof UEA updated)) {
            return;
        }

        recordEvent(
                KnownAuditActions.UEA_UPDATED.name(),
                resolveActorId(),
                resolveActorRole(),
                updated.getGraduateProgramId(),
                AuditSeverityLevel.STANDARD,
                "ueaId=%d,clave=%s,graduateProgramId=%d".formatted(
                        updated.getId(), updated.getClave(), updated.getGraduateProgramId()));
    }

    @AfterReturning(pointcut = "deactivateUeaPointcut()", returning = "result")
    public void auditUeaDeactivated(Object result) {
        if (!(result instanceof UEA deactivated)) {
            return;
        }

        recordEvent(
                KnownAuditActions.UEA_DEACTIVATED.name(),
                resolveActorId(),
                resolveActorRole(),
                deactivated.getGraduateProgramId(),
                AuditSeverityLevel.STANDARD,
                "ueaId=%d,clave=%s,graduateProgramId=%d".formatted(
                        deactivated.getId(), deactivated.getClave(), deactivated.getGraduateProgramId()));
    }

    @AfterReturning(pointcut = "restoreUeaPointcut()", returning = "result")
    public void auditUeaRestored(Object result) {
        if (!(result instanceof UEA restored)) {
            return;
        }

        recordEvent(
                KnownAuditActions.UEA_RESTORED.name(),
                resolveActorId(),
                resolveActorRole(),
                restored.getGraduateProgramId(),
                AuditSeverityLevel.STANDARD,
                "ueaId=%d,clave=%s,graduateProgramId=%d".formatted(
                        restored.getId(), restored.getClave(), restored.getGraduateProgramId()));
    }

    @AfterThrowing(pointcut = "ueaControllerPointcut()", throwing = "ex")
    public void auditUeaAccessDenied(AccessDeniedException ex) {
        recordEvent(
                KnownAuditActions.RBAC_VIOLATION_DETECTED.name(),
                resolveActorId(),
                resolveActorRole(),
                TenantContext.get(),
                AuditSeverityLevel.HIGH,
                ex.getMessage());
    }

    private Long resolveActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveActorRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return RoleType.COORDINATOR.name();
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replace("ROLE_", ""))
                .orElse(RoleType.COORDINATOR.name());
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
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

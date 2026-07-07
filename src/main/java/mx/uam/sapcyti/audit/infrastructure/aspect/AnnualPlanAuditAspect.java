package mx.uam.sapcyti.audit.infrastructure.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.model.AuditSeverity;
import mx.uam.sapcyti.audit.domain.model.AuditSeverityLevel;
import mx.uam.sapcyti.audit.domain.model.KnownAuditActions;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.planning.application.service.ChangeStatusUseCase;
import mx.uam.sapcyti.planning.application.service.CreateAnnualPlanUseCase;
import mx.uam.sapcyti.planning.application.service.ExportAnnualPlanUseCase;
import mx.uam.sapcyti.planning.application.service.SaveEntriesUseCase;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
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

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AnnualPlanAuditAspect {

    private final AuditOutputPort auditOutputPort;

    @Pointcut("execution(* mx.uam.sapcyti.planning.application.service.CreateAnnualPlanUseCase.execute(..))")
    public void createAnnualPlanPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.planning.application.service.SaveEntriesUseCase.execute(..))")
    public void saveEntriesPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.planning.application.service.ChangeStatusUseCase.execute(..))")
    public void changeStatusPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.planning.application.service.ExportAnnualPlanUseCase.execute(..))")
    public void exportAnnualPlanPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.planning.infrastructure.adapter.in.AnnualPlanController.*(..))")
    public void annualPlanControllerPointcut() {}

    @AfterReturning(pointcut = "createAnnualPlanPointcut()", returning = "result")
    public void auditAnnualPlanCreated(Object result) {
        if (!(result instanceof AnnualPlan plan)) {
            return;
        }
        recordEvent(
                KnownAuditActions.ANNUAL_PLAN_CREATED.name(),
                resolveActorId(),
                resolveActorRole(),
                plan.getGraduateProgramId(),
                AuditSeverityLevel.STANDARD,
                "year=%d,status=%s".formatted(plan.getYear(), plan.getStatus()));
    }

    @AfterReturning(pointcut = "saveEntriesPointcut()", returning = "result")
    public void auditEntriesSaved(Object result) {
        if (!(result instanceof AnnualPlan plan)) {
            return;
        }
        recordEvent(
                KnownAuditActions.ANNUAL_PLAN_ENTRIES_SAVED.name(),
                resolveActorId(),
                resolveActorRole(),
                plan.getGraduateProgramId(),
                AuditSeverityLevel.STANDARD,
                "year=%d,entries=%d".formatted(plan.getYear(), plan.getEntries().size()));
    }

    @AfterReturning(pointcut = "changeStatusPointcut()", returning = "result")
    public void auditStatusChanged(Object result) {
        if (!(result instanceof AnnualPlan plan)) {
            return;
        }
        recordEvent(
                KnownAuditActions.ANNUAL_PLAN_STATUS_CHANGED.name(),
                resolveActorId(),
                resolveActorRole(),
                plan.getGraduateProgramId(),
                AuditSeverityLevel.STANDARD,
                "year=%d,status=%s".formatted(plan.getYear(), plan.getStatus()));
    }

    @AfterReturning(pointcut = "exportAnnualPlanPointcut()", returning = "result")
    public void auditAnnualPlanExported(Object result) {
        if (!(result instanceof ExportAnnualPlanUseCase.ExportResult exported)) {
            return;
        }
        recordEvent(
                KnownAuditActions.ANNUAL_PLAN_EXPORTED.name(),
                resolveActorId(),
                resolveActorRole(),
                TenantContext.get(),
                AuditSeverityLevel.STANDARD,
                "filename=%s".formatted(exported.filename()));
    }

    @AfterThrowing(pointcut = "annualPlanControllerPointcut()", throwing = "ex")
    public void auditAnnualPlanAccessDenied(AccessDeniedException ex) {
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

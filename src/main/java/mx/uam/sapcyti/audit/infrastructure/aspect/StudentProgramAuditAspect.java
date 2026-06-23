package mx.uam.sapcyti.audit.infrastructure.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.academic.application.service.GetStudentProgramUseCase;
import mx.uam.sapcyti.academic.application.service.UpdateStudentProgramUseCase;
import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.model.AuditSeverity;
import mx.uam.sapcyti.audit.domain.model.AuditSeverityLevel;
import mx.uam.sapcyti.audit.domain.model.KnownAuditActions;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
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
public class StudentProgramAuditAspect {

    private final AuditOutputPort auditOutputPort;

    @Pointcut("execution(* mx.uam.sapcyti.academic.application.service.UpdateStudentProgramUseCase.execute(..))")
    public void updateStudentProgramPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.academic.infrastructure.adapter.in.StudentProgramController.*(..))")
    public void studentProgramControllerPointcut() {}

    @AfterReturning(pointcut = "updateStudentProgramPointcut()", returning = "result")
    public void auditStudentProgramUpdated(Object result) {
        if (!(result instanceof GetStudentProgramUseCase.StudentProgramDetail detail)) {
            return;
        }

        recordEvent(
                KnownAuditActions.STUDENT_PROGRAM_UPDATED.name(),
                resolveActorId(),
                resolveActorRole(),
                detail.graduateProgramId(),
                AuditSeverityLevel.STANDARD,
                "programId=%d,studentId=%d,tutorId=%s,advisorIds=%s".formatted(
                        detail.id(),
                        detail.studentId(),
                        detail.tutorId(),
                        detail.advisorIds()));
    }

    @AfterThrowing(pointcut = "studentProgramControllerPointcut()", throwing = "ex")
    public void auditStudentProgramAccessDenied(AccessDeniedException ex) {
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

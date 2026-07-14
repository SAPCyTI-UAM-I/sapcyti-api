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
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.survey.application.service.CloseSurveyUseCase;
import mx.uam.sapcyti.survey.application.service.CreateSurveyUseCase;
import mx.uam.sapcyti.survey.application.service.DeleteSurveyUseCase;
import mx.uam.sapcyti.survey.application.service.SubmitSurveyResponseUseCase;
import mx.uam.sapcyti.survey.application.service.SurveyDetail;
import mx.uam.sapcyti.survey.application.service.SubmittedResponseView;
import mx.uam.sapcyti.survey.application.service.UpdateSurveyUseCase;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.EnrollmentSurveyController;
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
public class SurveyAuditAspect {

    private final AuditOutputPort auditOutputPort;

    @Pointcut("execution(* mx.uam.sapcyti.survey.application.service.CreateSurveyUseCase.execute(..))")
    public void createSurveyPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.survey.application.service.UpdateSurveyUseCase.execute(..))")
    public void updateSurveyPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.survey.application.service.CloseSurveyUseCase.execute(..))")
    public void closeSurveyPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.survey.application.service.DeleteSurveyUseCase.execute(..))")
    public void deleteSurveyPointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.survey.application.service.SubmitSurveyResponseUseCase.execute(..))")
    public void submitResponsePointcut() {}

    @Pointcut("execution(* mx.uam.sapcyti.survey.infrastructure.adapter.in.EnrollmentSurveyController.*(..))")
    public void surveyControllerPointcut() {}

    @AfterReturning(pointcut = "createSurveyPointcut()", returning = "result")
    public void auditSurveyCreated(Object result) {
        if (!(result instanceof SurveyDetail detail)) {
            return;
        }
        recordEvent(
                KnownAuditActions.SURVEY_CREATED.name(),
                detail.getSurvey().getGraduateProgramId(),
                "surveyId=%d,term=%s".formatted(detail.getSurvey().getId(), detail.getSurvey().getTerm()));
    }

    @AfterReturning(pointcut = "updateSurveyPointcut()", returning = "result")
    public void auditSurveyUpdated(Object result) {
        if (!(result instanceof SurveyDetail detail)) {
            return;
        }
        recordEvent(
                KnownAuditActions.SURVEY_UPDATED.name(),
                detail.getSurvey().getGraduateProgramId(),
                "surveyId=%d,term=%s".formatted(detail.getSurvey().getId(), detail.getSurvey().getTerm()));
    }

    @AfterReturning(pointcut = "closeSurveyPointcut()", returning = "result")
    public void auditSurveyClosed(Object result) {
        if (!(result instanceof SurveyDetail detail)) {
            return;
        }
        recordEvent(
                KnownAuditActions.SURVEY_CLOSED.name(),
                detail.getSurvey().getGraduateProgramId(),
                "surveyId=%d,term=%s".formatted(detail.getSurvey().getId(), detail.getSurvey().getTerm()));
    }

    @AfterReturning(pointcut = "deleteSurveyPointcut()")
    public void auditSurveyDeleted() {
        recordEvent(
                KnownAuditActions.SURVEY_DELETED.name(),
                TenantContext.get(),
                "survey deleted");
    }

    @AfterReturning(pointcut = "submitResponsePointcut()", returning = "result")
    public void auditResponseSubmitted(Object result) {
        if (!(result instanceof SubmittedResponseView view)) {
            return;
        }
        recordEvent(
                KnownAuditActions.SURVEY_RESPONSE_SUBMITTED.name(),
                TenantContext.get(),
                "mode=%s,totalUeas=%d".formatted(view.getMode(), view.getTotalUeas()));
    }

    @AfterThrowing(pointcut = "surveyControllerPointcut()", throwing = "ex")
    public void auditSurveyAccessDenied(AccessDeniedException ex) {
        recordEvent(
                KnownAuditActions.RBAC_VIOLATION_DETECTED.name(),
                TenantContext.get(),
                ex.getMessage());
    }

    private void recordEvent(String action, Long programId, String details) {
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = request != null ? request.getRemoteAddr() : "unknown";

        AuditEvent event = AuditEvent.builder()
                .timestamp(Instant.now())
                .action(action)
                .actorId(resolveActorId())
                .actorRole(resolveActorRole())
                .graduateProgramId(programId)
                .severity(new AuditSeverity(AuditSeverityLevel.STANDARD))
                .ipAddress(ipAddress)
                .details(details)
                .build();

        auditOutputPort.record(event);
        log.info("AUDIT: {}", event.getAction());
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

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

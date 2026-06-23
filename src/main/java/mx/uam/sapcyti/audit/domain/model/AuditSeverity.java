package mx.uam.sapcyti.audit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Value Object indicating audit event importance.
 */
@Embeddable
public record AuditSeverity(
    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level", nullable = false)
    AuditSeverityLevel level
) {}

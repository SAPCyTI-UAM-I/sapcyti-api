package mx.uam.sapcyti.audit.domain.model;

/**
 * HIGH: security events (login, RBAC violations, password changes).
 * STANDARD: domain mutations (entity CRUD).
 * LOW: read operations (configurable per environment).
 */
public enum AuditSeverityLevel {
    HIGH,
    STANDARD,
    LOW
}

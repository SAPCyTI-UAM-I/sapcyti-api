package mx.uam.sapcyti.identity.domain.model;

/**
 * Enum of system roles.
 * Hierarchy: SYSTEM_ADMIN > COORDINATOR > ASSISTANT > PROFESSOR > STUDENT > SPEAKER.
 */
public enum RoleType {
    SYSTEM_ADMIN,
    COORDINATOR,
    ASSISTANT,
    PROFESSOR,
    STUDENT,
    SPEAKER
}

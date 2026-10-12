package mx.uam.sapcyti.configuration.domain.model;

/**
 * Application-wide mail dispatch mode (HU-35).
 *
 * <p>Source of truth: {@code program-configuration.schema.json#/definitions/MailMode}.
 * Codes, not labels. Never scoped by graduate program.
 */
public enum MailMode {
    DISABLED,
    TEST,
    ENABLED
}

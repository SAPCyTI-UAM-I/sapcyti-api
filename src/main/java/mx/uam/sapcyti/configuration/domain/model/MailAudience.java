package mx.uam.sapcyti.configuration.domain.model;

/**
 * Audience declared by the flow that creates a mail attempt (HU-35).
 *
 * <p>Source of truth: {@code program-configuration.schema.json#/definitions/MailAudience}.
 * The gate does not infer it from the address domain. {@code STAFF} covers professor,
 * coordination and office. A missing or other audience blocks dispatch.
 */
public enum MailAudience {
    STUDENT,
    STAFF
}

package mx.uam.sapcyti.configuration.domain.model;

/**
 * Result of one dispatch attempt against the global mail policy (HU-35).
 *
 * <p>Source of truth:
 * {@code program-configuration.schema.json#/definitions/MailDispatchOutcome}.
 * {@link #SENT} is provider acceptance, not mailbox delivery. {@link #SUPPRESSED}
 * is terminal. {@link #BLOCKED} means the provider was not contacted.
 */
public enum MailDispatchOutcome {
    SENT,
    SUPPRESSED,
    BLOCKED
}

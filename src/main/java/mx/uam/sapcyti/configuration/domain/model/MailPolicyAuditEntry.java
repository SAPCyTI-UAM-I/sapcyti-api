package mx.uam.sapcyti.configuration.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Internal history of one effective global mail-policy change (HU-35).
 *
 * <p>Source of truth:
 * {@code program-configuration.schema.json#/definitions/MailPolicyAuditEntry}.
 * Written in the same transaction as {@link MailPolicy}. No public endpoint.
 * No graduate program id, secrets, or message bodies.
 */
@Entity
@Table(name = "mail_policy_audit_entries")
public class MailPolicyAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_mode", nullable = false, length = 16)
    private MailMode previousMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_mode", nullable = false, length = 16)
    private MailMode newMode;

    @Column(name = "previous_version", nullable = false)
    private int previousVersion;

    @Column(name = "new_version", nullable = false)
    private int newVersion;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected MailPolicyAuditEntry() {
        // JPA
    }

    /**
     * Records one effective change. Version moves by exactly one and the mode differs.
     *
     * @param previousMode    mode before the change
     * @param newMode         mode after the change
     * @param previousVersion version before the change
     * @param newVersion      version after the change; previous + 1
     * @param actorUserId     user who confirmed the change; references users(id)
     * @param occurredAt      UTC of the commit
     */
    public MailPolicyAuditEntry(
            MailMode previousMode,
            MailMode newMode,
            int previousVersion,
            int newVersion,
            Long actorUserId,
            Instant occurredAt) {
        if (previousMode == null || newMode == null) {
            throw new IllegalArgumentException("Audit modes are required");
        }
        if (previousMode == newMode) {
            throw new IllegalArgumentException(
                    "Audit entry records an effective mode change");
        }
        if (previousVersion < 0 || newVersion < 0) {
            throw new IllegalArgumentException("Audit versions must be >= 0");
        }
        if (newVersion != previousVersion + 1) {
            throw new IllegalArgumentException("Audit entry does not skip versions");
        }
        if (actorUserId == null) {
            throw new IllegalArgumentException("Actor is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("UTC timestamp is required");
        }
        this.previousMode = previousMode;
        this.newMode = newMode;
        this.previousVersion = previousVersion;
        this.newVersion = newVersion;
        this.actorUserId = actorUserId;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public MailMode getPreviousMode() {
        return previousMode;
    }

    public MailMode getNewMode() {
        return newMode;
    }

    public int getPreviousVersion() {
        return previousVersion;
    }

    public int getNewVersion() {
        return newVersion;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}

package mx.uam.sapcyti.configuration.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Single application-wide mail policy row (HU-35).
 *
 * <p>Source of truth: {@code program-configuration.schema.json#/definitions/MailPolicy}.
 * Not a {@link ConfigurationParameter}. The primary key is a fixed singleton, not a
 * graduate program id. There is no {@code graduateProgramId} on this row.
 *
 * <p>Installation seed: {@link MailMode#DISABLED}, version 0, null actor and timestamp.
 * Version increments only on an effective mode change.
 */
@Entity
@Table(name = "mail_policy")
public class MailPolicy {

    /** Fixed primary key. The table allows this value alone. */
    public static final int SINGLETON_ID = 1;

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 16)
    private MailMode mode;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    protected MailPolicy() {
        // JPA
    }

    private MailPolicy(MailMode mode, int version, Instant updatedAt, Long updatedByUserId) {
        this.id = SINGLETON_ID;
        this.mode = requireMode(mode);
        if (version < 0) {
            throw new IllegalArgumentException("Mail policy version must be >= 0");
        }
        this.version = version;
        if (version == 0) {
            if (updatedAt != null || updatedByUserId != null) {
                throw new IllegalArgumentException(
                        "Version 0 keeps updatedAt and updatedByUserId null");
            }
        } else if (updatedAt == null || updatedByUserId == null) {
            throw new IllegalArgumentException(
                    "An effective change records actor and UTC");
        }
        this.updatedAt = updatedAt;
        this.updatedByUserId = updatedByUserId;
    }

    /**
     * Installation row: DISABLED, version 0, null audit fields.
     *
     * @return the singleton seed
     */
    public static MailPolicy initial() {
        return new MailPolicy(MailMode.DISABLED, 0, null, null);
    }

    /**
     * Applies an effective mode change. Same mode is rejected here; the port treats
     * that case as a no-op before calling this method. Version becomes previous + 1.
     *
     * @param newMode      mode distinct from the current one
     * @param actorUserId  authenticated actor; references users(id)
     * @param occurredAt   UTC of the change
     */
    public void changeMode(MailMode newMode, Long actorUserId, Instant occurredAt) {
        MailMode target = requireMode(newMode);
        if (target == this.mode) {
            throw new IllegalArgumentException(
                    "Same mode is not an effective mail policy change");
        }
        if (actorUserId == null) {
            throw new IllegalArgumentException(
                    "Actor is required for an effective mail policy change");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "UTC timestamp is required for an effective mail policy change");
        }
        this.mode = target;
        this.version = this.version + 1;
        this.updatedAt = occurredAt;
        this.updatedByUserId = actorUserId;
    }

    public int getId() {
        return id;
    }

    public MailMode getMode() {
        return mode;
    }

    public int getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    private static MailMode requireMode(MailMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mail mode is required");
        }
        return mode;
    }
}

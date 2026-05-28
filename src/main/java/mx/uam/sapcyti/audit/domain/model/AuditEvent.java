package mx.uam.sapcyti.audit.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Aggregate Root. Records a system event for compliance and traceability.
 */
@Entity
@Table(name = "audit_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "actor_id")
    private Long actorId; // Nullable for anonymous/failed login (BC-06)

    @Column(name = "actor_role")
    private String actorRole;

    @Column(nullable = false)
    private String action; // E.g. "LOGIN_SUCCESS"

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 2000)
    private String details; // JSON format extra data

    @Embedded
    private AuditSeverity severity;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "graduate_program_id")
    private Long graduateProgramId; // Nullable for system-level events
}

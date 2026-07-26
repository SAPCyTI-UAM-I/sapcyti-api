package mx.uam.sapcyti.trimestral.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "trimestral_plan_outdated_reasons",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trimestral_plan_id", "reason"}))
public class PlanOutdatedReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trimestral_plan_id", nullable = false)
    private TrimestralPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private OutdatedReason reason;

    protected PlanOutdatedReason() {
        // For JPA
    }

    private PlanOutdatedReason(TrimestralPlan plan, OutdatedReason reason) {
        this.plan = plan;
        this.reason = reason;
    }

    public static PlanOutdatedReason create(TrimestralPlan plan, OutdatedReason reason) {
        return new PlanOutdatedReason(plan, reason);
    }

    void assignPlan(TrimestralPlan plan) {
        this.plan = plan;
    }

    public Long getId() {
        return id;
    }

    public OutdatedReason getReason() {
        return reason;
    }
}

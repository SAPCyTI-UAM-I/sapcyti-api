package mx.uam.sapcyti.planning.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.planning.domain.exception.InvalidStatusTransitionException;
import mx.uam.sapcyti.planning.domain.exception.PlanNotEditableException;

@Entity
@Table(name = "annual_plans", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"graduate_program_id", "year"})
})
public class AnnualPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "graduate_program_id", nullable = false)
    private Long graduateProgramId;

    @Column(name = "year", nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AnnualPlanStatus status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    private List<AnnualPlanEntry> entries = new ArrayList<>();

    protected AnnualPlan() {
        // For JPA
    }

    private AnnualPlan(Long graduateProgramId, int year, Long createdBy) {
        this.graduateProgramId = graduateProgramId;
        this.year = year;
        this.status = AnnualPlanStatus.BORRADOR;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public static AnnualPlan create(
            Long graduateProgramId,
            int year,
            Long createdBy,
            List<UEA> activeUeas,
            Map<Long, AnnualPlanEntry> previousEntriesByUeaId) {
        validateYear(year);
        AnnualPlan plan = new AnnualPlan(graduateProgramId, year, createdBy);
        short position = 1;
        for (UEA uea : activeUeas) {
            AnnualPlanEntry previous = uea.getId() == null
                    ? null
                    : previousEntriesByUeaId.get(uea.getId());
            AnnualPlanEntry entry = AnnualPlanEntry.createWithPreload(plan, uea, position++, previous);
            plan.entries.add(entry);
        }
        return plan;
    }

    public static List<String> deriveTerms(int year) {
        String suffix = String.format("%02d", year % 100);
        return List.of(suffix + "-I", suffix + "-P", suffix + "-O");
    }

    public void transitionTo(AnnualPlanStatus newStatus) {
        if (status == newStatus || !isAdjacentTransition(status, newStatus)) {
            throw new InvalidStatusTransitionException();
        }
        this.status = newStatus;
    }

    public void assertEditable() {
        if (status != AnnualPlanStatus.BORRADOR) {
            throw new PlanNotEditableException();
        }
    }

    public AnnualPlanEntry requireEntry(Long entryId) {
        return entries.stream()
                .filter(entry -> entry.getId().equals(entryId))
                .findFirst()
                .orElseThrow(mx.uam.sapcyti.planning.domain.exception.AnnualPlanEntryNotFoundException::new);
    }

    public Map<Long, AnnualPlanEntry> entriesByUeaId() {
        return entries.stream().collect(Collectors.toMap(AnnualPlanEntry::getUeaId, Function.identity()));
    }

    public void replaceEntries(List<AnnualPlanEntry> newEntries) {
        entries.clear();
        short position = 1;
        for (AnnualPlanEntry entry : newEntries.stream()
                .sorted(Comparator.comparing(AnnualPlanEntry::getPosicion))
                .toList()) {
            entry.setPosicion(position++);
            entry.assignPlan(this);
            entries.add(entry);
        }
    }

    private static boolean isAdjacentTransition(AnnualPlanStatus from, AnnualPlanStatus to) {
        return switch (from) {
            case BORRADOR -> to == AnnualPlanStatus.TERMINADA;
            case TERMINADA -> to == AnnualPlanStatus.BORRADOR || to == AnnualPlanStatus.ARCHIVADA;
            case ARCHIVADA -> to == AnnualPlanStatus.TERMINADA;
        };
    }

    private static void validateYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("year must be between 2000 and 2100");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getGraduateProgramId() {
        return graduateProgramId;
    }

    public int getYear() {
        return year;
    }

    public AnnualPlanStatus getStatus() {
        return status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<AnnualPlanEntry> getEntries() {
        return List.copyOf(entries);
    }
}

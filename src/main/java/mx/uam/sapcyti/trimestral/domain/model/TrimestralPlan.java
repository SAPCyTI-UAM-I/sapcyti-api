package mx.uam.sapcyti.trimestral.domain.model;

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
import org.hibernate.annotations.BatchSize;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import mx.uam.sapcyti.trimestral.domain.exception.InvalidTrimestralStatusTransitionException;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotEditableException;

@Entity
@Table(name = "trimestral_plans", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"graduate_program_id", "term"})
})
public class TrimestralPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "graduate_program_id", nullable = false)
    private Long graduateProgramId;

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @Column(name = "term", nullable = false, length = 4)
    private String term;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private TrimestralPlanStatus status;

    @Column(name = "outdated", nullable = false)
    private boolean outdated;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @BatchSize(size = 50)
    private List<TrimestralPlanGroup> groups = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<PlanWarning> warnings = new ArrayList<>();

    protected TrimestralPlan() {
        // For JPA
    }

    private TrimestralPlan(Long graduateProgramId, Long surveyId, String term, Long createdBy) {
        this.graduateProgramId = graduateProgramId;
        this.surveyId = surveyId;
        this.term = term;
        this.status = TrimestralPlanStatus.BORRADOR;
        this.outdated = false;
        this.createdBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static TrimestralPlan create(Long graduateProgramId, Long surveyId, String term, Long createdBy) {
        return new TrimestralPlan(graduateProgramId, surveyId, term, createdBy);
    }

    public void transitionTo(TrimestralPlanStatus newStatus) {
        if (status == newStatus || !isAdjacentTransition(status, newStatus)) {
            throw new InvalidTrimestralStatusTransitionException();
        }
        this.status = newStatus;
        touch();
    }

    public void assertEditable() {
        if (status != TrimestralPlanStatus.BORRADOR) {
            throw new TrimestralPlanNotEditableException();
        }
    }

    public void markOutdated() {
        this.outdated = true;
        touch();
    }

    public void clearOutdated() {
        this.outdated = false;
        touch();
    }

    public void replaceGroups(List<TrimestralPlanGroup> newGroups) {
        groups.clear();
        short position = 1;
        for (TrimestralPlanGroup group : newGroups) {
            group.setPosicion(position++);
            group.assignPlan(this);
            groups.add(group);
        }
        touch();
    }

    public void replaceWarnings(List<PlanWarning> newWarnings) {
        warnings.clear();
        for (PlanWarning warning : newWarnings) {
            warning.assignPlan(this);
            warnings.add(warning);
        }
        touch();
    }

    public void clearContent() {
        groups.clear();
        warnings.clear();
        touch();
    }

    public Set<Long> assignedStudentIds() {
        return groups.stream()
                .flatMap(group -> group.getStudents().stream())
                .map(GroupStudent::getStudentId)
                .collect(Collectors.toSet());
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    private static boolean isAdjacentTransition(TrimestralPlanStatus from, TrimestralPlanStatus to) {
        return switch (from) {
            case BORRADOR -> to == TrimestralPlanStatus.TERMINADA;
            case TERMINADA -> to == TrimestralPlanStatus.BORRADOR;
        };
    }

    public static int yearFromTerm(String term) {
        return 2000 + Integer.parseInt(term.substring(0, 2));
    }

    public static char trimesterLetter(String term) {
        return Character.toUpperCase(term.charAt(2));
    }

    public static Comparator<String> termNewestFirst() {
        return Comparator.comparingInt((String t) -> Integer.parseInt(t.substring(0, 2)))
                .thenComparingInt(t -> trimesterOrder(t.charAt(2)))
                .reversed();
    }

    private static int trimesterOrder(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'I' -> 1;
            case 'P' -> 2;
            case 'O' -> 3;
            default -> 0;
        };
    }

    public Long getId() {
        return id;
    }

    public Long getGraduateProgramId() {
        return graduateProgramId;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public String getTerm() {
        return term;
    }

    public TrimestralPlanStatus getStatus() {
        return status;
    }

    public boolean isOutdated() {
        return outdated;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<TrimestralPlanGroup> getGroups() {
        return List.copyOf(groups);
    }

    public List<PlanWarning> getWarnings() {
        return List.copyOf(warnings);
    }
}

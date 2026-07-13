package mx.uam.sapcyti.survey.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Entity
@Table(name = "enrollment_surveys", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"graduate_program_id", "term"})
})
public class EnrollmentSurvey {

    private static final Pattern TERM_PATTERN = Pattern.compile("^[0-9]{2}[OIP]$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "graduate_program_id", nullable = false)
    private Long graduateProgramId;

    @Column(name = "term", nullable = false, length = 4)
    private String term;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @Column(name = "intro_message", length = 500)
    private String introMessage;

    @Column(name = "closed_manually", nullable = false)
    private boolean closedManually;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "survey_snapshot_ueas",
            joinColumns = @JoinColumn(name = "survey_id"))
    @Column(name = "uea_id")
    private Set<Long> snapshotUeaIds = new HashSet<>();

    protected EnrollmentSurvey() {
        // For JPA
    }

    private EnrollmentSurvey(
            Long graduateProgramId,
            String term,
            Instant opensAt,
            Instant closesAt,
            String introMessage,
            Long createdBy,
            List<Long> snapshotUeaIds) {
        this.graduateProgramId = graduateProgramId;
        this.term = term;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.introMessage = introMessage;
        this.closedManually = false;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.snapshotUeaIds = new HashSet<>(snapshotUeaIds);
    }

    public static EnrollmentSurvey create(
            Long graduateProgramId,
            String term,
            Instant opensAt,
            Instant closesAt,
            String introMessage,
            Long createdBy,
            List<Long> snapshotUeaIds) {
        validateTerm(term);
        validateDates(opensAt, closesAt);
        return new EnrollmentSurvey(
                graduateProgramId, term, opensAt, closesAt, introMessage, createdBy, snapshotUeaIds);
    }

    public SurveyStatus getStatus(Instant now) {
        if (closedManually || !now.isBefore(closesAt)) {
            return SurveyStatus.CERRADO;
        }
        if (now.isBefore(opensAt)) {
            return SurveyStatus.PROGRAMADO;
        }
        return SurveyStatus.ACTIVO;
    }

    public void updateSchedule(Instant opensAt, Instant closesAt, String introMessage) {
        validateDates(opensAt, closesAt);
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.introMessage = introMessage;
    }

    public void reopen(Instant opensAt, Instant closesAt, String introMessage, List<Long> snapshotUeaIds) {
        validateDates(opensAt, closesAt);
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.introMessage = introMessage;
        this.closedManually = false;
        replaceSnapshot(snapshotUeaIds);
    }

    public void closeManually() {
        this.closedManually = true;
    }

    public void replaceSnapshot(List<Long> ueaIds) {
        this.snapshotUeaIds = new HashSet<>(ueaIds);
    }

    public static void validateTerm(String term) {
        if (term == null || term.isBlank() || !TERM_PATTERN.matcher(term).matches()) {
            throw new IllegalArgumentException("term must match format AA[O|I|P]");
        }
    }

    public static void validateDates(Instant opensAt, Instant closesAt) {
        if (opensAt == null || closesAt == null) {
            throw new IllegalArgumentException("opensAt and closesAt are required");
        }
        if (!closesAt.isAfter(opensAt)) {
            throw new IllegalArgumentException("closesAt must be after opensAt");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getGraduateProgramId() {
        return graduateProgramId;
    }

    public String getTerm() {
        return term;
    }

    public Instant getOpensAt() {
        return opensAt;
    }

    public Instant getClosesAt() {
        return closesAt;
    }

    public String getIntroMessage() {
        return introMessage;
    }

    public boolean isClosedManually() {
        return closedManually;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Long> getSnapshotUeaIds() {
        return Set.copyOf(snapshotUeaIds);
    }
}

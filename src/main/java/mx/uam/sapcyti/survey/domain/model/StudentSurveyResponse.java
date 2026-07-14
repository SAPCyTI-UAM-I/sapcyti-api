package mx.uam.sapcyti.survey.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "survey_responses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"survey_id", "student_id"})
})
public class StudentSurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_term", nullable = false, length = 4)
    private AcademicTerm academicTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private SurveyResponseMode mode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "survey_response_ueas",
            joinColumns = @JoinColumn(name = "response_id"))
    @Column(name = "uea_id")
    private List<Long> ueaIds = new ArrayList<>();

    protected StudentSurveyResponse() {
        // For JPA
    }

    private StudentSurveyResponse(
            Long surveyId,
            Long studentId,
            AcademicTerm academicTerm,
            SurveyResponseMode mode,
            List<Long> ueaIds) {
        this.surveyId = surveyId;
        this.studentId = studentId;
        this.academicTerm = academicTerm;
        this.mode = mode;
        this.ueaIds = new ArrayList<>(ueaIds);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static StudentSurveyResponse create(
            Long surveyId,
            Long studentId,
            AcademicTerm academicTerm,
            SurveyResponseMode mode,
            List<Long> ueaIds) {
        return new StudentSurveyResponse(surveyId, studentId, academicTerm, mode, ueaIds);
    }

    public void replace(
            AcademicTerm academicTerm,
            SurveyResponseMode mode,
            List<Long> ueaIds) {
        this.academicTerm = academicTerm;
        this.mode = mode;
        this.ueaIds = new ArrayList<>(ueaIds);
        this.updatedAt = Instant.now();
    }

    public int getTotalUeas() {
        return ueaIds.size();
    }

    public Long getId() {
        return id;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public AcademicTerm getAcademicTerm() {
        return academicTerm;
    }

    public SurveyResponseMode getMode() {
        return mode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Long> getUeaIds() {
        return List.copyOf(ueaIds);
    }
}

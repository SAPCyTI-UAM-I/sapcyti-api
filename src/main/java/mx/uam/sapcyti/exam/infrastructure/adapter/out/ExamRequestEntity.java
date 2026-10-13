package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamType;

/**
 * Row of {@code exam_requests}. The id is reserved before the object put.
 * This cut inserts only {@code BORRADOR}. {@code folio} and {@code submittedAt} stay null in that state.
 */
@Entity
@Table(name = "exam_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uq_exam_requests_student_type", columnNames = {"student_id", "exam_type"}),
        @UniqueConstraint(name = "uq_exam_requests_folio", columnNames = "folio")
})
class ExamRequestEntity {

    @Id
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 16)
    private ExamType examType;

    @Column(name = "graduate_program_id", nullable = false)
    private Long graduateProgramId;

    @Enumerated(EnumType.STRING)
    @Column(name = "degree", nullable = false, length = 20)
    private Degree degree;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ExamRequestStatus status;

    @Column(name = "folio", length = 17)
    private String folio;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    protected ExamRequestEntity() {
        // JPA
    }

    static ExamRequestEntity draft(
            long id,
            long studentId,
            ExamType examType,
            long graduateProgramId,
            Degree degree) {
        ExamRequestEntity entity = new ExamRequestEntity();
        entity.id = id;
        entity.studentId = studentId;
        entity.examType = examType;
        entity.graduateProgramId = graduateProgramId;
        entity.degree = degree;
        entity.status = ExamRequestStatus.BORRADOR;
        entity.folio = null;
        entity.version = 1;
        entity.submittedAt = null;
        return entity;
    }

    static ExamRequestEntity stored(
            long id,
            long studentId,
            ExamType examType,
            long graduateProgramId,
            Degree degree,
            ExamRequestStatus status,
            String folio,
            int version,
            Instant submittedAt) {
        ExamRequestEntity entity = new ExamRequestEntity();
        entity.id = id;
        entity.studentId = studentId;
        entity.examType = examType;
        entity.graduateProgramId = graduateProgramId;
        entity.degree = degree;
        entity.status = status;
        entity.folio = folio;
        entity.version = version;
        entity.submittedAt = submittedAt;
        return entity;
    }

    void incrementVersion() {
        this.version = this.version + 1;
    }

    Long getId() {
        return id;
    }

    Long getStudentId() {
        return studentId;
    }

    ExamType getExamType() {
        return examType;
    }

    Long getGraduateProgramId() {
        return graduateProgramId;
    }

    Degree getDegree() {
        return degree;
    }

    ExamRequestStatus getStatus() {
        return status;
    }

    String getFolio() {
        return folio;
    }

    int getVersion() {
        return version;
    }

    Instant getSubmittedAt() {
        return submittedAt;
    }
}

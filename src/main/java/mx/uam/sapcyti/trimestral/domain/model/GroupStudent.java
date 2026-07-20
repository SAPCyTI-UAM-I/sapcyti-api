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

@Entity
@Table(name = "trimestral_plan_group_students")
public class GroupStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private TrimestralPlanGroup group;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "enrollment_id", nullable = false, length = 20)
    private String enrollmentId;

    @Column(name = "full_name", nullable = false, length = 300)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 6)
    private StudentSource source;

    @Column(name = "academic_term", length = 4)
    private String academicTerm;

    @Column(name = "posicion", nullable = false)
    private short posicion;

    protected GroupStudent() {
        // For JPA
    }

    private GroupStudent(
            TrimestralPlanGroup group,
            Long studentId,
            String enrollmentId,
            String fullName,
            StudentSource source,
            String academicTerm,
            short posicion) {
        this.group = group;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.fullName = fullName;
        this.source = source;
        this.academicTerm = academicTerm;
        this.posicion = posicion;
    }

    public static GroupStudent create(
            TrimestralPlanGroup group,
            Long studentId,
            String enrollmentId,
            String fullName,
            StudentSource source,
            String academicTerm,
            short posicion) {
        return new GroupStudent(group, studentId, enrollmentId, fullName, source, academicTerm, posicion);
    }

    void assignGroup(TrimestralPlanGroup group) {
        this.group = group;
    }

    void setPosicion(short posicion) {
        this.posicion = posicion;
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public String getFullName() {
        return fullName;
    }

    public StudentSource getSource() {
        return source;
    }

    public String getAcademicTerm() {
        return academicTerm;
    }

    public short getPosicion() {
        return posicion;
    }
}

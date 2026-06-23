package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Aggregate root for enrolled students (HU-15).
 */
@Entity
@Table(name = "students", uniqueConstraints = {
        @UniqueConstraint(columnNames = "enrollment_id"),
        @UniqueConstraint(columnNames = "user_id")
})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false, length = 20)
    private String enrollmentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "graduate_program_id", nullable = false)
    private Long graduateProgramId;

    @Column(name = "advisor_id")
    private Long advisorId;

    @Embedded
    private PersonalData personalData;

    @Embedded
    private AcademicInformation academicInformation;

    protected Student() {
        // For JPA
    }

    public Student(
            String enrollmentId,
            Long userId,
            Long graduateProgramId,
            Long advisorId,
            PersonalData personalData,
            AcademicInformation academicInformation) {
        this.enrollmentId = enrollmentId;
        this.userId = userId;
        this.graduateProgramId = graduateProgramId;
        this.advisorId = advisorId;
        this.personalData = personalData;
        this.academicInformation = academicInformation;
    }

    public Long getId() {
        return id;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGraduateProgramId() {
        return graduateProgramId;
    }

    public Long getAdvisorId() {
        return advisorId;
    }

    public PersonalData getPersonalData() {
        return personalData;
    }

    public AcademicInformation getAcademicInformation() {
        return academicInformation;
    }
}

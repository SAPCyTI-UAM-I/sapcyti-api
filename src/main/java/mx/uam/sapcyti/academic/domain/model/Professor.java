package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Aggregate root for academic staff (HU-21).
 */
@Entity
@Table(name = "professors", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id")
})
public class Professor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "professor_type", nullable = false, length = 20)
    private ProfessorType professorType;

    @Column(name = "employee_number", length = 20)
    private String employeeNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "graduate_program_id", nullable = false)
    private Long graduateProgramId;

    @Embedded
    private PersonalData personalData;

    @Embedded
    private ProfessorInformation professorInformation;

    protected Professor() {
        // For JPA
    }

    public Professor(
            ProfessorType professorType,
            String employeeNumber,
            Long userId,
            Long graduateProgramId,
            PersonalData personalData,
            ProfessorInformation professorInformation) {
        this.professorType = professorType;
        this.employeeNumber = employeeNumber;
        this.userId = userId;
        this.graduateProgramId = graduateProgramId;
        this.personalData = personalData;
        this.professorInformation = professorInformation;
    }

    public Long getId() {
        return id;
    }

    public ProfessorType getProfessorType() {
        return professorType;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGraduateProgramId() {
        return graduateProgramId;
    }

    public PersonalData getPersonalData() {
        return personalData;
    }

    public ProfessorInformation getProfessorInformation() {
        return professorInformation;
    }

    public void updatePersonalData(PersonalData personalData) {
        this.personalData = personalData;
    }

    public void updateProfessorInformation(ProfessorInformation professorInformation) {
        this.professorInformation = professorInformation;
    }

    public void updateTypeAndEmployeeNumber(ProfessorType professorType, String employeeNumber) {
        this.professorType = professorType;
        this.employeeNumber = employeeNumber;
    }
}

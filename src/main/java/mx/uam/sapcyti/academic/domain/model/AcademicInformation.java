package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

/**
 * Student program data: undergraduate degree, program type, admission date (HU-15).
 */
@Embeddable
public class AcademicInformation {

    @Column(name = "undergraduate_degree", nullable = false, length = 200)
    private String undergraduateDegree;

    @Enumerated(EnumType.STRING)
    @Column(name = "program_type", nullable = false, length = 50)
    private ProgramType programType;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    protected AcademicInformation() {
        // For JPA
    }

    public AcademicInformation(String undergraduateDegree, ProgramType programType, LocalDate admissionDate) {
        this.undergraduateDegree = undergraduateDegree;
        this.programType = programType;
        this.admissionDate = admissionDate;
    }

    public String getUndergraduateDegree() {
        return undergraduateDegree;
    }

    public ProgramType getProgramType() {
        return programType;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }
}

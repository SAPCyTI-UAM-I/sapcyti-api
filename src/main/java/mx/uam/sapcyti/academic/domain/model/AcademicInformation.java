package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

/**
 * Student program data: undergraduate degree, last degree obtained, program type,
 * admission date, and admission term (HU-15 / HU-56).
 */
@Embeddable
public class AcademicInformation {

    @Column(name = "undergraduate_degree", nullable = false, length = 200)
    private String undergraduateDegree;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_degree_obtained", nullable = false, length = 20)
    private DegreeLevel lastDegreeObtained;

    @Enumerated(EnumType.STRING)
    @Column(name = "program_type", nullable = false, length = 50)
    private ProgramType programType;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    /** Nullable for legacy rows predating V19; required on API create/update. */
    @Column(name = "admission_term", length = 4)
    private String admissionTerm;

    protected AcademicInformation() {
        // For JPA
    }

    public AcademicInformation(
            String undergraduateDegree,
            DegreeLevel lastDegreeObtained,
            ProgramType programType,
            LocalDate admissionDate,
            String admissionTerm) {
        this.undergraduateDegree = undergraduateDegree;
        this.lastDegreeObtained = lastDegreeObtained;
        this.programType = programType;
        this.admissionDate = admissionDate;
        this.admissionTerm = admissionTerm;
    }

    public String getUndergraduateDegree() {
        return undergraduateDegree;
    }

    public DegreeLevel getLastDegreeObtained() {
        return lastDegreeObtained;
    }

    public ProgramType getProgramType() {
        return programType;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public String getAdmissionTerm() {
        return admissionTerm;
    }
}

package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/**
 * Professor employment data: commission membership and sabbatical schedule (HU-21).
 */
@Embeddable
public class ProfessorInformation {

    @Column(name = "commission_member", nullable = false)
    private boolean commissionMember;

    @Column(name = "next_sabbatical_start")
    private LocalDate nextSabbaticalStart;

    @Column(name = "next_sabbatical_end")
    private LocalDate nextSabbaticalEnd;

    protected ProfessorInformation() {
        // For JPA
    }

    public ProfessorInformation(
            boolean commissionMember, LocalDate nextSabbaticalStart, LocalDate nextSabbaticalEnd) {
        this.commissionMember = commissionMember;
        this.nextSabbaticalStart = nextSabbaticalStart;
        this.nextSabbaticalEnd = nextSabbaticalEnd;
    }

    public boolean isCommissionMember() {
        return commissionMember;
    }

    public LocalDate getNextSabbaticalStart() {
        return nextSabbaticalStart;
    }

    public LocalDate getNextSabbaticalEnd() {
        return nextSabbaticalEnd;
    }
}

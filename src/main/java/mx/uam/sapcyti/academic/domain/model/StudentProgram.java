package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Aggregate root for a student's academic program enrollment (HU-19, HU-20).
 */
@Entity
@Table(name = "student_programs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "program_type"})
})
public class StudentProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "graduate_program_id", nullable = false)
    private Long graduateProgramId;

    @Column(name = "enrollment_id", nullable = false, length = 20)
    private String enrollmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "program_type", nullable = false, length = 20)
    private ProgramType programType;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    @Column(name = "research_area", length = 200)
    private String researchArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramStatus status;

    @Column(name = "withdrawal_reason", length = 500)
    private String withdrawalReason;

    @Column(name = "tutor_id")
    private Long tutorId;

    @OneToMany(mappedBy = "studentProgram", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "position")
    private List<StudentProgramAdvisor> advisors = new ArrayList<>();

    protected StudentProgram() {
        // For JPA
    }

    public StudentProgram(
            Long studentId,
            Long graduateProgramId,
            String enrollmentId,
            ProgramType programType,
            LocalDate admissionDate,
            ProgramStatus status) {
        this.studentId = studentId;
        this.graduateProgramId = graduateProgramId;
        this.enrollmentId = enrollmentId;
        this.programType = programType;
        this.admissionDate = admissionDate;
        this.status = status;
    }

    public static StudentProgram forRegistration(
            Long studentId,
            Long graduateProgramId,
            String enrollmentId,
            ProgramType programType,
            LocalDate admissionDate,
            Long advisorId) {
        StudentProgram program = new StudentProgram(
                studentId,
                graduateProgramId,
                enrollmentId,
                programType,
                admissionDate,
                ProgramStatus.ACTIVO);
        if (advisorId != null) {
            program.replaceAdvisors(List.of(advisorId));
        }
        return program;
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getGraduateProgramId() {
        return graduateProgramId;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public ProgramType getProgramType() {
        return programType;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public LocalDate getGraduationDate() {
        return graduationDate;
    }

    public String getResearchArea() {
        return researchArea;
    }

    public ProgramStatus getStatus() {
        return status;
    }

    public String getWithdrawalReason() {
        return withdrawalReason;
    }

    public Long getTutorId() {
        return tutorId;
    }

    public List<Long> getAdvisorIds() {
        return advisors.stream().map(StudentProgramAdvisor::getProfessorId).toList();
    }

    public void updateMetadata(
            LocalDate admissionDate,
            LocalDate graduationDate,
            String researchArea,
            ProgramStatus status,
            String withdrawalReason,
            Long tutorId) {
        this.admissionDate = admissionDate;
        this.graduationDate = graduationDate;
        this.researchArea = researchArea;
        this.status = status;
        this.withdrawalReason = withdrawalReason;
        this.tutorId = tutorId;
        validateDates();
        validateWithdrawal();
    }

    public void replaceAdvisors(List<Long> advisorIds) {
        advisors.clear();
        int position = 0;
        for (Long professorId : advisorIds) {
            StudentProgramAdvisor advisor = new StudentProgramAdvisor(this, professorId, position++);
            advisors.add(advisor);
        }
    }

    public void validateDates() {
        if (graduationDate != null && graduationDate.isBefore(admissionDate)) {
            throw new IllegalArgumentException("Graduation date must be on or after admission date");
        }
    }

    public void validateWithdrawal() {
        if (status == ProgramStatus.BAJA && (withdrawalReason == null || withdrawalReason.isBlank())) {
            throw new IllegalArgumentException("Withdrawal reason is required when status is BAJA");
        }
    }

    public static void validateUniqueAdvisorIds(List<Long> advisorIds) {
        Set<Long> unique = new HashSet<>(advisorIds);
        if (unique.size() != advisorIds.size()) {
            throw new IllegalArgumentException("Duplicate advisor IDs are not allowed");
        }
    }
}

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
@Table(name = "trimestral_plan_warnings")
public class PlanWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trimestral_plan_id", nullable = false)
    private TrimestralPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 40)
    private WarningCode code;

    @Column(name = "clave", length = 20)
    private String clave;

    @Column(name = "enrollment_id", length = 20)
    private String enrollmentId;

    @Column(name = "employee_number", length = 20)
    private String employeeNumber;

    @Column(name = "group_id")
    private Long groupId;

    protected PlanWarning() {
        // For JPA
    }

    private PlanWarning(
            TrimestralPlan plan,
            WarningCode code,
            String clave,
            String enrollmentId,
            String employeeNumber,
            Long groupId) {
        this.plan = plan;
        this.code = code;
        this.clave = clave;
        this.enrollmentId = enrollmentId;
        this.employeeNumber = employeeNumber;
        this.groupId = groupId;
    }

    public static PlanWarning of(
            TrimestralPlan plan,
            WarningCode code,
            String clave,
            String enrollmentId,
            String employeeNumber,
            Long groupId) {
        return new PlanWarning(plan, code, clave, enrollmentId, employeeNumber, groupId);
    }

    void assignPlan(TrimestralPlan plan) {
        this.plan = plan;
    }

    public Long getId() {
        return id;
    }

    public WarningCode getCode() {
        return code;
    }

    public String getClave() {
        return clave;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}

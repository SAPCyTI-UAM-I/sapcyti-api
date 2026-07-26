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
import jakarta.persistence.UniqueConstraint;

@Entity
// La restricción vive en V20; declararla aquí también hace que el esquema generado en
// pruebas la tenga, que es donde se detectan las colisiones al recrear la demanda.
@Table(name = "trimestral_plan_unassigned_demand", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_trimestral_unassigned_pair",
                columnNames = {"trimestral_plan_id", "uea_id", "student_id"})
})
public class UnassignedDemand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trimestral_plan_id", nullable = false)
    private TrimestralPlan plan;

    @Column(name = "uea_id", nullable = false)
    private Long ueaId;

    @Column(name = "clave", nullable = false, length = 20)
    private String clave;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "enrollment_id", nullable = false, length = 20)
    private String enrollmentId;

    @Column(name = "full_name", nullable = false, length = 300)
    private String fullName;

    @Column(name = "academic_term", length = 4)
    private String academicTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private UnassignedDemandReason reason;

    @Column(name = "posicion", nullable = false)
    private short posicion;

    protected UnassignedDemand() {
        // For JPA
    }

    private UnassignedDemand(
            TrimestralPlan plan,
            Long ueaId,
            String clave,
            String nombre,
            Long studentId,
            String enrollmentId,
            String fullName,
            String academicTerm,
            UnassignedDemandReason reason,
            short posicion) {
        this.plan = plan;
        this.ueaId = ueaId;
        this.clave = clave;
        this.nombre = nombre;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.fullName = fullName;
        this.academicTerm = academicTerm;
        this.reason = reason;
        this.posicion = posicion;
    }

    public static UnassignedDemand create(
            TrimestralPlan plan,
            Long ueaId,
            String clave,
            String nombre,
            Long studentId,
            String enrollmentId,
            String fullName,
            String academicTerm,
            UnassignedDemandReason reason,
            short posicion) {
        return new UnassignedDemand(
                plan,
                ueaId,
                clave,
                nombre,
                studentId,
                enrollmentId,
                fullName,
                academicTerm,
                reason,
                posicion);
    }

    void assignPlan(TrimestralPlan plan) {
        this.plan = plan;
    }

    void setPosicion(short posicion) {
        this.posicion = posicion;
    }

    public Long getId() {
        return id;
    }

    public Long getUeaId() {
        return ueaId;
    }

    public String getClave() {
        return clave;
    }

    public String getNombre() {
        return nombre;
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

    public String getAcademicTerm() {
        return academicTerm;
    }

    public UnassignedDemandReason getReason() {
        return reason;
    }

    public short getPosicion() {
        return posicion;
    }
}

package mx.uam.sapcyti.trimestral.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A professor assigned to a group (HU-57/59). Research groups have co-directors, so a
 * group carries an ordered list of these; {@code employeeNumber}/{@code professorName}
 * are snapshots, like {@link GroupStudent}. The Excel stacks them in the NEMP/PROF rows.
 */
@Entity
@Table(
        name = "trimestral_plan_group_professors",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"group_id", "professor_id"}),
            @UniqueConstraint(columnNames = {"group_id", "posicion"})
        })
public class GroupProfessor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private TrimestralPlanGroup group;

    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Column(name = "employee_number", length = 20)
    private String employeeNumber;

    @Column(name = "professor_name", nullable = false, length = 300)
    private String professorName;

    @Column(name = "posicion", nullable = false)
    private short posicion;

    protected GroupProfessor() {
        // For JPA
    }

    private GroupProfessor(
            TrimestralPlanGroup group,
            Long professorId,
            String employeeNumber,
            String professorName,
            short posicion) {
        this.group = group;
        this.professorId = professorId;
        this.employeeNumber = employeeNumber;
        this.professorName = professorName;
        this.posicion = posicion;
    }

    public static GroupProfessor create(
            TrimestralPlanGroup group,
            Long professorId,
            String employeeNumber,
            String professorName,
            short posicion) {
        return new GroupProfessor(group, professorId, employeeNumber, professorName, posicion);
    }

    void assignGroup(TrimestralPlanGroup group) {
        this.group = group;
    }

    void setPosicion(short posicion) {
        this.posicion = posicion;
    }

    public void refreshSnapshot(String employeeNumber, String professorName) {
        this.employeeNumber = employeeNumber;
        this.professorName = professorName;
    }

    public Long getId() {
        return id;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getProfessorName() {
        return professorName;
    }

    public short getPosicion() {
        return posicion;
    }
}

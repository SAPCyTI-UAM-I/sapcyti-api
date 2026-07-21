package mx.uam.sapcyti.trimestral.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "trimestral_plan_groups")
public class TrimestralPlanGroup {

    public static final Pattern GROUP_QUOTA_PATTERN = Pattern.compile("^\\*$|^[1-9][0-9]*$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trimestral_plan_id", nullable = false)
    private TrimestralPlan plan;

    @Column(name = "uea_id", nullable = false)
    private Long ueaId;

    @Column(name = "posicion", nullable = false)
    private short posicion;

    @Column(name = "clave", nullable = false, length = 20)
    private String clave;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "tipo_uea", nullable = false, length = 20)
    private String tipoUea;

    @Column(name = "grupo", length = 10)
    private String grupo;

    @Column(name = "cupo", length = 5)
    private String cupo;

    @Column(name = "lun_ini", length = 5)
    private String lunIni;

    @Column(name = "lun_fin", length = 5)
    private String lunFin;

    @Column(name = "lun_lab", nullable = false)
    private boolean lunLab;

    @Column(name = "mar_ini", length = 5)
    private String marIni;

    @Column(name = "mar_fin", length = 5)
    private String marFin;

    @Column(name = "mar_lab", nullable = false)
    private boolean marLab;

    @Column(name = "mie_ini", length = 5)
    private String mieIni;

    @Column(name = "mie_fin", length = 5)
    private String mieFin;

    @Column(name = "mie_lab", nullable = false)
    private boolean mieLab;

    @Column(name = "jue_ini", length = 5)
    private String jueIni;

    @Column(name = "jue_fin", length = 5)
    private String jueFin;

    @Column(name = "jue_lab", nullable = false)
    private boolean jueLab;

    @Column(name = "vie_ini", length = 5)
    private String vieIni;

    @Column(name = "vie_fin", length = 5)
    private String vieFin;

    @Column(name = "vie_lab", nullable = false)
    private boolean vieLab;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @BatchSize(size = 50)
    private List<GroupStudent> students = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @BatchSize(size = 50)
    private List<GroupProfessor> professors = new ArrayList<>();

    protected TrimestralPlanGroup() {
        // For JPA
    }

    private TrimestralPlanGroup(
            TrimestralPlan plan,
            Long ueaId,
            short posicion,
            String clave,
            String nombre,
            String tipoUea,
            String grupo,
            String cupo) {
        this.plan = plan;
        this.ueaId = ueaId;
        this.posicion = posicion;
        this.clave = clave;
        this.nombre = nombre;
        this.tipoUea = tipoUea;
        this.grupo = grupo;
        this.cupo = cupo;
        this.lunLab = false;
        this.marLab = false;
        this.mieLab = false;
        this.jueLab = false;
        this.vieLab = false;
    }

    public static TrimestralPlanGroup createProposed(
            TrimestralPlan plan,
            Long ueaId,
            short posicion,
            String clave,
            String nombre,
            String tipoUea,
            String grupo,
            String cupo) {
        return new TrimestralPlanGroup(plan, ueaId, posicion, clave, nombre, tipoUea, grupo, cupo);
    }

    public static TrimestralPlanGroup createEdited(
            TrimestralPlan plan,
            Long ueaId,
            short posicion,
            String clave,
            String nombre,
            String tipoUea,
            String grupo,
            String cupo,
            Map<ScheduleDay, DaySlot> schedule) {
        TrimestralPlanGroup group =
                new TrimestralPlanGroup(plan, ueaId, posicion, clave, nombre, tipoUea, grupo, cupo);
        group.applySchedule(schedule);
        return group;
    }

    public void addStudent(GroupStudent student) {
        student.assignGroup(this);
        students.add(student);
    }

    public void replaceStudents(List<GroupStudent> newStudents) {
        students.clear();
        short position = 1;
        for (GroupStudent student : newStudents) {
            student.setPosicion(position++);
            student.assignGroup(this);
            students.add(student);
        }
    }

    public void addProfessor(GroupProfessor professor) {
        professor.assignGroup(this);
        professors.add(professor);
    }

    public void replaceProfessors(List<GroupProfessor> newProfessors) {
        professors.clear();
        short position = 1;
        for (GroupProfessor professor : newProfessors) {
            professor.setPosicion(position++);
            professor.assignGroup(this);
            professors.add(professor);
        }
    }

    public void applySchedule(Map<ScheduleDay, DaySlot> schedule) {
        if (schedule == null || schedule.size() != 5) {
            throw new IllegalArgumentException("schedule must contain exactly 5 days LUN..VIE");
        }
        for (ScheduleDay day : ScheduleDay.values()) {
            if (!schedule.containsKey(day)) {
                throw new IllegalArgumentException("schedule missing day " + day);
            }
        }
        DaySlot lun = schedule.get(ScheduleDay.LUN);
        DaySlot mar = schedule.get(ScheduleDay.MAR);
        DaySlot mie = schedule.get(ScheduleDay.MIE);
        DaySlot jue = schedule.get(ScheduleDay.JUE);
        DaySlot vie = schedule.get(ScheduleDay.VIE);
        validateSlot(lun);
        validateSlot(mar);
        validateSlot(mie);
        validateSlot(jue);
        validateSlot(vie);
        this.lunIni = lun.start();
        this.lunFin = lun.end();
        this.lunLab = lun.lab();
        this.marIni = mar.start();
        this.marFin = mar.end();
        this.marLab = mar.lab();
        this.mieIni = mie.start();
        this.mieFin = mie.end();
        this.mieLab = mie.lab();
        this.jueIni = jue.start();
        this.jueFin = jue.end();
        this.jueLab = jue.lab();
        this.vieIni = vie.start();
        this.vieFin = vie.end();
        this.vieLab = vie.lab();
    }

    public static void validateGrupo(String grupo) {
        if (grupo != null && grupo.length() > 10) {
            throw new IllegalArgumentException("grupo must be at most 10 characters");
        }
    }

    public static void validateCupo(String cupo) {
        if (cupo != null && !GROUP_QUOTA_PATTERN.matcher(cupo).matches()) {
            throw new IllegalArgumentException("cupo must be a positive integer or *");
        }
    }

    public static void validateSlot(DaySlot slot) {
        if (slot == null) {
            throw new IllegalArgumentException("schedule day slot is required");
        }
        validateTime(slot.start());
        validateTime(slot.end());
        if (slot.start() != null && slot.end() != null && slot.start().compareTo(slot.end()) > 0) {
            throw new IllegalArgumentException("schedule start must be <= end");
        }
    }

    private static void validateTime(String time) {
        if (time != null && !TIME_PATTERN.matcher(time).matches()) {
            throw new IllegalArgumentException("schedule time must match HH:mm");
        }
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

    public short getPosicion() {
        return posicion;
    }

    public String getClave() {
        return clave;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipoUea() {
        return tipoUea;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getCupo() {
        return cupo;
    }

    public List<GroupProfessor> getProfessors() {
        return List.copyOf(professors);
    }

    public List<GroupStudent> getStudents() {
        return List.copyOf(students);
    }

    public Map<ScheduleDay, DaySlot> scheduleMap() {
        Map<ScheduleDay, DaySlot> map = new EnumMap<>(ScheduleDay.class);
        map.put(ScheduleDay.LUN, new DaySlot(lunIni, lunFin, lunLab));
        map.put(ScheduleDay.MAR, new DaySlot(marIni, marFin, marLab));
        map.put(ScheduleDay.MIE, new DaySlot(mieIni, mieFin, mieLab));
        map.put(ScheduleDay.JUE, new DaySlot(jueIni, jueFin, jueLab));
        map.put(ScheduleDay.VIE, new DaySlot(vieIni, vieFin, vieLab));
        return map;
    }

    public List<DaySlot> scheduleInOrder() {
        return List.of(
                new DaySlot(lunIni, lunFin, lunLab),
                new DaySlot(marIni, marFin, marLab),
                new DaySlot(mieIni, mieFin, mieLab),
                new DaySlot(jueIni, jueFin, jueLab),
                new DaySlot(vieIni, vieFin, vieLab));
    }

    public boolean exceedsCupo() {
        if (cupo == null || "*".equals(cupo)) {
            return false;
        }
        try {
            int limit = Integer.parseInt(cupo);
            return students.size() > limit;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static Comparator<GroupStudentSnapshot> surnameComparator() {
        return Comparator.comparing(GroupStudentSnapshot::firstLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(GroupStudentSnapshot::secondLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(GroupStudentSnapshot::firstName, Comparator.nullsLast(String::compareToIgnoreCase));
    }

    public record DaySlot(String start, String end, boolean lab) {
    }

    public record GroupStudentSnapshot(
            Long studentId,
            String enrollmentId,
            String fullName,
            String firstName,
            String firstLastName,
            String secondLastName,
            StudentSource source,
            String academicTerm) {
    }
}

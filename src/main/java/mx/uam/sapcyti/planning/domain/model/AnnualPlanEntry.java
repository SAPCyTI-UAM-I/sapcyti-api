package mx.uam.sapcyti.planning.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ConstraintMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.model.UeaType;

@Entity
@Table(name = "annual_plan_entries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"annual_plan_id", "uea_id"})
})
public class AnnualPlanEntry {

    private static final Pattern GROUP_QUOTA_PATTERN = Pattern.compile("^\\*$|^[1-9][0-9]*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "annual_plan_id",
            nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private AnnualPlan plan;

    @Column(name = "uea_id", nullable = false)
    private Long ueaId;

    @Column(name = "posicion", nullable = false)
    private short posicion;

    @Column(name = "clave", nullable = false, length = 20)
    private String clave;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "modalidad", nullable = false, length = 10)
    private String modalidad;

    @Column(name = "grupos_i", length = 5)
    private String gruposI;

    @Column(name = "cupo_i", length = 5)
    private String cupoI;

    @Column(name = "grupos_p", length = 5)
    private String gruposP;

    @Column(name = "cupo_p", length = 5)
    private String cupoP;

    @Column(name = "grupos_o", length = 5)
    private String gruposO;

    @Column(name = "cupo_o", length = 5)
    private String cupoO;

    @Column(name = "p_fis", length = 3)
    private String pFis;

    @Column(name = "p_mat", length = 3)
    private String pMat;

    @Column(name = "mcmai", length = 3)
    private String mcmai;

    @Column(name = "p_quim", length = 3)
    private String pQuim;

    @Column(name = "p_iquim", length = 3)
    private String pIquim;

    @Column(name = "p_ibiom", length = 3)
    private String pIbiom;

    @Column(name = "pcyti", length = 3)
    private String pcyti;

    @Column(name = "pema", length = 3)
    private String pema;

    @Column(name = "efmc", length = 3)
    private String efmc;

    protected AnnualPlanEntry() {
        // For JPA
    }

    private AnnualPlanEntry(
            AnnualPlan plan,
            Long ueaId,
            short posicion,
            String clave,
            String nombre,
            String modalidad) {
        this.plan = plan;
        this.ueaId = ueaId;
        this.posicion = posicion;
        this.clave = clave;
        this.nombre = nombre;
        this.modalidad = modalidad;
    }

    public static AnnualPlanEntry createEmpty(AnnualPlan plan, UEA uea, short posicion) {
        AnnualPlanEntry entry = new AnnualPlanEntry(
                plan,
                uea.getId(),
                posicion,
                uea.getClave(),
                uea.getNombre(),
                uea.getModalidad().name());
        entry.pcyti = derivePcyti(uea);
        return entry;
    }

    public static AnnualPlanEntry createWithPreload(
            AnnualPlan plan, UEA uea, short posicion, AnnualPlanEntry previous) {
        AnnualPlanEntry entry = createEmpty(plan, uea, posicion);
        if (previous != null) {
            entry.copyEditableValuesFrom(previous);
        }
        return entry;
    }

    public void refreshSnapshot(UEA uea) {
        this.nombre = uea.getNombre();
        this.modalidad = uea.getModalidad().name();
        this.pcyti = derivePcyti(uea);
    }

    public void updateValues(
            String gruposI,
            String cupoI,
            String gruposP,
            String cupoP,
            String gruposO,
            String cupoO,
            Map<GraduateProgramMark, String> marks) {
        this.gruposI = validateGroupQuota("gruposI", gruposI);
        this.cupoI = validateGroupQuota("cupoI", cupoI);
        this.gruposP = validateGroupQuota("gruposP", gruposP);
        this.cupoP = validateGroupQuota("cupoP", cupoP);
        this.gruposO = validateGroupQuota("gruposO", gruposO);
        this.cupoO = validateGroupQuota("cupoO", cupoO);
        applyMarks(marks);
    }

    private void copyEditableValuesFrom(AnnualPlanEntry previous) {
        this.gruposI = previous.gruposI;
        this.cupoI = previous.cupoI;
        this.gruposP = previous.gruposP;
        this.cupoP = previous.cupoP;
        this.gruposO = previous.gruposO;
        this.cupoO = previous.cupoO;
        this.pFis = previous.pFis;
        this.pMat = previous.pMat;
        this.mcmai = previous.mcmai;
        this.pQuim = previous.pQuim;
        this.pIquim = previous.pIquim;
        this.pIbiom = previous.pIbiom;
        // pcyti is not copied: it stays derived from the catalog `tipo` (set in createEmpty).
        this.pema = previous.pema;
        this.efmc = previous.efmc;
    }

    private void applyMarks(Map<GraduateProgramMark, String> marks) {
        this.pFis = null;
        this.pMat = null;
        this.mcmai = null;
        this.pQuim = null;
        this.pIquim = null;
        this.pIbiom = null;
        this.pema = null;
        this.efmc = null;
        // pcyti is derived from the catalog `tipo` (read-only) — never cleared nor set from the payload.
        if (marks == null) {
            return;
        }
        for (Map.Entry<GraduateProgramMark, String> mark : marks.entrySet()) {
            if (mark.getKey() == GraduateProgramMark.PCYTI) {
                continue; // derived from tipo; ignore any client-provided value
            }
            String value = validateMark(mark.getKey().name(), mark.getValue());
            setMark(mark.getKey(), value);
        }
    }

    private static String validateGroupQuota(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!GROUP_QUOTA_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid value for " + field + ": " + value);
        }
        return value;
    }

    private static String validateMark(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.equals("X") && !value.equals("O") && !value.equals("X/O")) {
            throw new IllegalArgumentException("Invalid value for marks." + field + ": " + value);
        }
        return value;
    }

    // PCYTI is derived from the catalog type: OBLIGATORIA -> "X", OPTATIVA -> "O" (read-only).
    private static String derivePcyti(UEA uea) {
        return uea.getTipo() == UeaType.OBLIGATORIA ? "X" : "O";
    }

    private void setMark(GraduateProgramMark mark, String value) {
        switch (mark) {
            case P_FIS -> this.pFis = value;
            case P_MAT -> this.pMat = value;
            case MCMAI -> this.mcmai = value;
            case P_QUIM -> this.pQuim = value;
            case P_IQUIM -> this.pIquim = value;
            case P_IBIOM -> this.pIbiom = value;
            case PCYTI -> this.pcyti = value;
            case PEMA -> this.pema = value;
            case EFMC -> this.efmc = value;
        }
    }

    public Map<GraduateProgramMark, String> getMarks() {
        Map<GraduateProgramMark, String> marks = new EnumMap<>(GraduateProgramMark.class);
        putIfPresent(marks, GraduateProgramMark.P_FIS, pFis);
        putIfPresent(marks, GraduateProgramMark.P_MAT, pMat);
        putIfPresent(marks, GraduateProgramMark.MCMAI, mcmai);
        putIfPresent(marks, GraduateProgramMark.P_QUIM, pQuim);
        putIfPresent(marks, GraduateProgramMark.P_IQUIM, pIquim);
        putIfPresent(marks, GraduateProgramMark.P_IBIOM, pIbiom);
        putIfPresent(marks, GraduateProgramMark.PCYTI, pcyti);
        putIfPresent(marks, GraduateProgramMark.PEMA, pema);
        putIfPresent(marks, GraduateProgramMark.EFMC, efmc);
        return marks;
    }

    private static void putIfPresent(
            Map<GraduateProgramMark, String> marks, GraduateProgramMark key, String value) {
        if (value != null && !value.isBlank()) {
            marks.put(key, value);
        }
    }

    void assignPlan(AnnualPlan plan) {
        this.plan = plan;
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

    public void setPosicion(short posicion) {
        this.posicion = posicion;
    }

    public String getClave() {
        return clave;
    }

    public String getNombre() {
        return nombre;
    }

    public String getModalidad() {
        return modalidad;
    }

    public String getGruposI() {
        return gruposI;
    }

    public String getCupoI() {
        return cupoI;
    }

    public String getGruposP() {
        return gruposP;
    }

    public String getCupoP() {
        return cupoP;
    }

    public String getGruposO() {
        return gruposO;
    }

    public String getCupoO() {
        return cupoO;
    }

    public String getPFis() {
        return pFis;
    }

    public String getPMat() {
        return pMat;
    }

    public String getMcmai() {
        return mcmai;
    }

    public String getPQuim() {
        return pQuim;
    }

    public String getPIquim() {
        return pIquim;
    }

    public String getPIbiom() {
        return pIbiom;
    }

    public String getPcyti() {
        return pcyti;
    }

    public String getPema() {
        return pema;
    }

    public String getEfmc() {
        return efmc;
    }
}

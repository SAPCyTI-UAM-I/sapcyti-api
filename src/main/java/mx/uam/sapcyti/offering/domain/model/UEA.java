package mx.uam.sapcyti.offering.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.regex.Pattern;
import mx.uam.sapcyti.offering.domain.exception.ClaveInvalidFormatException;

/**
 * Aggregate root for the UEA catalog (HU-39, HU-46).
 */
@Entity
@Table(name = "ueas", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"graduate_program_id", "clave"})
})
public class UEA {

    private static final Pattern CLAVE_PATTERN = Pattern.compile("^\\d+$");
    private static final int CLAVE_MAX_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "graduate_program_id", nullable = false)
    private Long graduateProgramId;

    @Column(name = "clave", nullable = false, length = 20, updatable = false)
    private String clave;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private UeaType tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 10)
    private UeaModality modalidad;

    @Column(name = "horas_teoria", nullable = false, precision = 5, scale = 1)
    private BigDecimal horasTeoria;

    @Column(name = "horas_practica", nullable = false, precision = 5, scale = 1)
    private BigDecimal horasPractica;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_formacion", nullable = false, length = 20)
    private FormationType tipoFormacion;

    @Column(name = "creditos", nullable = false)
    private int creditos;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UEA() {
        // For JPA
    }

    private UEA(
            Long graduateProgramId,
            String clave,
            String nombre,
            UeaType tipo,
            UeaModality modalidad,
            BigDecimal horasTeoria,
            BigDecimal horasPractica,
            FormationType tipoFormacion,
            int creditos,
            boolean active) {
        this.graduateProgramId = graduateProgramId;
        this.clave = clave;
        this.nombre = nombre;
        this.tipo = tipo;
        this.modalidad = modalidad;
        this.horasTeoria = horasTeoria;
        this.horasPractica = horasPractica;
        this.tipoFormacion = tipoFormacion;
        this.creditos = creditos;
        this.active = active;
        this.createdAt = Instant.now();
    }

    public static UEA create(
            Long graduateProgramId,
            String clave,
            String nombre,
            UeaType tipo,
            UeaModality modalidad,
            BigDecimal horasTeoria,
            BigDecimal horasPractica,
            FormationType tipoFormacion,
            int creditos) {
        String normalizedClave = validateAndNormalizeClave(clave);
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre is required");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("tipo is required");
        }
        if (modalidad != UeaModality.MIXTA) {
            throw new IllegalArgumentException("modalidad must be MIXTA");
        }
        if (tipoFormacion == null) {
            throw new IllegalArgumentException("tipoFormacion is required");
        }
        validateHoras(horasTeoria, horasPractica);
        validateCreditos(creditos);

        return new UEA(
                graduateProgramId,
                normalizedClave,
                nombre.trim(),
                tipo,
                modalidad,
                horasTeoria,
                horasPractica,
                tipoFormacion,
                creditos,
                true);
    }

    public static String validateAndNormalizeClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("clave is required");
        }
        String trimmed = clave.trim();
        if (trimmed.length() > CLAVE_MAX_LENGTH || !CLAVE_PATTERN.matcher(trimmed).matches()) {
            throw new ClaveInvalidFormatException();
        }
        return trimmed;
    }

    private static void validateHoras(BigDecimal horasTeoria, BigDecimal horasPractica) {
        if (horasTeoria == null || horasPractica == null) {
            throw new IllegalArgumentException("horasTeoria and horasPractica are required");
        }
        if (horasTeoria.compareTo(BigDecimal.ZERO) < 0 || horasPractica.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("horas must be greater than or equal to 0");
        }
    }

    private static void validateCreditos(int creditos) {
        if (creditos <= 0) {
            throw new IllegalArgumentException("creditos must be greater than 0");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getGraduateProgramId() {
        return graduateProgramId;
    }

    public String getClave() {
        return clave;
    }

    public String getNombre() {
        return nombre;
    }

    public UeaType getTipo() {
        return tipo;
    }

    public UeaModality getModalidad() {
        return modalidad;
    }

    public BigDecimal getHorasTeoria() {
        return horasTeoria;
    }

    public BigDecimal getHorasPractica() {
        return horasPractica;
    }

    public FormationType getTipoFormacion() {
        return tipoFormacion;
    }

    public int getCreditos() {
        return creditos;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package mx.uam.sapcyti.configuration.domain.model;

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
import java.util.regex.Pattern;

/**
 * Value Object (persisted as Entity with surrogate id) — Program Configuration (BC-04).
 *
 * <p>Immutable key-value pair that externalizes a business rule of the graduate
 * program. Allows modifying dates, quotas, and criteria without changing source
 * code, in response to QA-3.
 *
 * <p>Source of truth:
 * {@code program-configuration.schema.json#/definitions/ConfigurationParameter}.
 *
 * <p>Column names use {@code param_key} and {@code param_value} to avoid
 * PostgreSQL reserved words ({@code key}, {@code value}).
 *
 * @see GraduateProgram
 * @see <a href="Architecture.md §4">Domain model — ConfigurationParameter</a>
 */
@Entity
@Table(name = "configuration_parameters",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_configuration_parameters_program_key",
                columnNames = {"graduate_program_id", "param_key"}))
public class ConfigurationParameter {

    /** UPPER_SNAKE_CASE pattern: starts with uppercase letter, then uppercase, digits, or underscores. */
    private static final Pattern KEY_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private static final int KEY_MAX_LENGTH = 100;
    private static final int VALUE_MAX_LENGTH = 500;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "graduate_program_id", nullable = false)
    private GraduateProgram graduateProgram;

    @Column(name = "param_key", nullable = false, length = KEY_MAX_LENGTH)
    private String key;

    @Column(name = "param_value", nullable = false, length = VALUE_MAX_LENGTH)
    private String value;

    @Column(length = DESCRIPTION_MAX_LENGTH)
    private String description;

    // --- JPA requires a no-arg constructor ---
    protected ConfigurationParameter() {
        // no-op
    }

    /**
     * Creates a new ConfigurationParameter with invariant validation.
     *
     * @param graduateProgram the owning program; must not be null
     * @param key             UPPER_SNAKE_CASE key; must not be blank, max 100 chars
     * @param value           parameter value; must not be blank, max 500 chars
     * @param description     optional human-readable description; max 500 chars
     * @throws IllegalArgumentException if invariants are violated
     */
    public ConfigurationParameter(GraduateProgram graduateProgram,
                                  String key,
                                  String value,
                                  String description) {
        if (graduateProgram == null) {
            throw new IllegalArgumentException(
                    "Graduate program is required");
        }
        this.graduateProgram = graduateProgram;
        setKey(key);
        setValue(value);
        setDescription(description);
    }

    public Long getId() {
        return id;
    }

    public GraduateProgram getGraduateProgram() {
        return graduateProgram;
    }

    public String getKey() {
        return key;
    }

    /**
     * Sets the parameter key.
     *
     * @param key must match {@code ^[A-Z][A-Z0-9_]*$}, max 100 characters
     * @throws IllegalArgumentException if key is invalid
     */
    public void setKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Parameter key is required");
        }
        if (key.length() > KEY_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Parameter key must not exceed "
                            + KEY_MAX_LENGTH + " characters");
        }
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Key must be in UPPER_SNAKE_CASE format");
        }
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    /**
     * Sets the parameter value.
     *
     * @param value must not be blank, max 500 characters
     * @throws IllegalArgumentException if value is invalid
     */
    public void setValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Parameter value is required");
        }
        if (value.length() > VALUE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Parameter value must not exceed "
                            + VALUE_MAX_LENGTH + " characters");
        }
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Sets the parameter description.
     *
     * @param description optional; max 500 characters; null is allowed
     */
    public void setDescription(String description) {
        if (description != null
                && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Description must not exceed "
                            + DESCRIPTION_MAX_LENGTH + " characters");
        }
        this.description = description;
    }
}

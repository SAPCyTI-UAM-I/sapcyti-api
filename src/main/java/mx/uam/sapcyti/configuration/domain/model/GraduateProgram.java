package mx.uam.sapcyti.configuration.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Aggregate Root — Program Configuration (BC-04).
 *
 * <p>Represents a UAM graduate program. Contains the parametric configuration
 * of business rules, enabling the multi-graduate program support required by QA-4.
 *
 * <p>Source of truth: {@code program-configuration.schema.json#/definitions/GraduateProgram}.
 *
 * @see <a href="Architecture.md §4">Domain model — GraduateProgram</a>
 */
@Entity
@Table(
    name = "graduate_programs",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_graduate_program_name",
        columnNames = {"name"}))
public class GraduateProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String division;

    @OneToMany(mappedBy = "graduateProgram",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<ConfigurationParameter> configurationParameters =
        new ArrayList<>();

    // --- JPA requires a no-arg constructor ---
    protected GraduateProgram() {
        // no-op
    }

    /**
     * Creates a new GraduateProgram with invariant validation.
     *
     * @param name     program name; must not be blank, max 200 characters
     * @param division academic division; must not be blank, max 100 characters
     * @throws IllegalArgumentException if invariants are violated
     */
    public GraduateProgram(String name, String division) {
        setName(name);
        setDivision(division);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Updates the program name.
     *
     * @param name must not be blank, max 200 characters
     * @throws IllegalArgumentException if name is blank or exceeds max length
     */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Program name is required");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException(
                "Program name must not exceed 200 characters");
        }
        this.name = name.trim();
    }

    public String getDivision() {
        return division;
    }

    /**
     * Updates the academic division.
     *
     * @param division must not be blank, max 100 characters
     * @throws IllegalArgumentException if division is blank or exceeds max length
     */
    public void setDivision(String division) {
        if (division == null || division.isBlank()) {
            throw new IllegalArgumentException("Division is required");
        }
        if (division.length() > 100) {
            throw new IllegalArgumentException(
                "Division must not exceed 100 characters");
        }
        this.division = division.trim();
    }

    /**
     * Returns an unmodifiable view of the configuration parameters.
     *
     * @return configuration parameters for this program
     */
    public List<ConfigurationParameter> getConfigurationParameters() {
        return Collections.unmodifiableList(configurationParameters);
    }
}

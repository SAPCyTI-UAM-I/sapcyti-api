package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Reference data: research area under a line of knowledge (HU-44).
 */
@Entity
@Table(name = "research_areas", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"line_id", "name"})
})
public class ResearchArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private ResearchLine line;

    @Column(nullable = false, length = 200)
    private String name;

    protected ResearchArea() {
        // For JPA
    }

    public Long getId() {
        return id;
    }

    public ResearchLine getLine() {
        return line;
    }

    public String getName() {
        return name;
    }
}

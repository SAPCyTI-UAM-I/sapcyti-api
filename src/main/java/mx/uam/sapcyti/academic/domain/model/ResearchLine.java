package mx.uam.sapcyti.academic.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Reference data: research line of knowledge (HU-44).
 */
@Entity
@Table(name = "research_lines")
public class ResearchLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    private List<ResearchArea> areas = new ArrayList<>();

    protected ResearchLine() {
        // For JPA
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<ResearchArea> getAreas() {
        return areas;
    }
}

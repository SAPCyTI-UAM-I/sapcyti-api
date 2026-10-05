package mx.uam.sapcyti.academic.infrastructure.adapter.out.repository;

import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.ResearchLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataResearchLineRepository extends JpaRepository<ResearchLine, Long> {

    Optional<ResearchLine> findByName(String name);

    @Query("SELECT rl FROM ResearchLine rl LEFT JOIN FETCH rl.areas ORDER BY rl.name ASC")
    java.util.List<ResearchLine> findAllWithAreas();
}

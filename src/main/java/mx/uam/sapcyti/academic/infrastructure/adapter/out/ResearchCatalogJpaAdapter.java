package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.port.out.ResearchCatalogPort;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataResearchLineRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ResearchCatalogJpaAdapter implements ResearchCatalogPort {

    private final SpringDataResearchLineRepository lineRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ResearchCatalogEntry> findAll() {
        return lineRepository.findAllWithAreas().stream()
                .map(line -> new ResearchCatalogEntry(
                        line.getName(),
                        line.getAreas().stream().map(area -> area.getName()).toList()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean lineExists(String lineName) {
        return lineRepository.findByName(lineName).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areaBelongsToLine(String lineName, String areaName) {
        return lineRepository.findByName(lineName)
                .map(line -> line.getAreas().stream().anyMatch(area -> area.getName().equals(areaName)))
                .orElse(false);
    }
}

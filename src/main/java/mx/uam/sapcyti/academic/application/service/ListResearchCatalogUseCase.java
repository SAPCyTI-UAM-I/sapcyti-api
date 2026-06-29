package mx.uam.sapcyti.academic.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.port.out.ResearchCatalogPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListResearchCatalogUseCase {

    private final ResearchCatalogPort researchCatalogPort;

    @Transactional(readOnly = true)
    public List<ResearchCatalogPort.ResearchCatalogEntry> execute() {
        return researchCatalogPort.findAll();
    }
}

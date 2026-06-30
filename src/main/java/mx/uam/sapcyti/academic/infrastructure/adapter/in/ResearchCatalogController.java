package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.service.ListResearchCatalogUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.ResearchCatalogItemResponse;
import mx.uam.sapcyti.academic.infrastructure.mapper.ResearchCatalogMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/research-catalog")
@RequiredArgsConstructor
@Tag(name = "Research Catalog", description = "HU-44 line of knowledge and research area hierarchy.")
public class ResearchCatalogController {

    private final ListResearchCatalogUseCase listResearchCatalogUseCase;
    private final ResearchCatalogMapper mapper;

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List research catalog", description = "Returns the HU-44 hierarchy as canonical Spanish strings.")
    public List<ResearchCatalogItemResponse> list() {
        return listResearchCatalogUseCase.execute().stream().map(mapper::toResponse).toList();
    }
}

package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * HU-44 research catalog hierarchy item.
 */
@Schema(name = "ResearchCatalogItem", description = "Line of knowledge with its research areas.")
public record ResearchCatalogItemResponse(
        @Schema(description = "Canonical line of knowledge name.") String line,
        @Schema(description = "Research areas belonging to this line.") List<String> areas) {
}

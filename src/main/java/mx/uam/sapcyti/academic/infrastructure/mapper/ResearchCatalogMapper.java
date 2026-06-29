package mx.uam.sapcyti.academic.infrastructure.mapper;

import mx.uam.sapcyti.academic.domain.port.out.ResearchCatalogPort;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.ResearchCatalogItemResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResearchCatalogMapper {

    ResearchCatalogItemResponse toResponse(ResearchCatalogPort.ResearchCatalogEntry entry);
}

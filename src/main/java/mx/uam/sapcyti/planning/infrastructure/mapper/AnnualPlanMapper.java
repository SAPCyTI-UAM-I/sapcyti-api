package mx.uam.sapcyti.planning.infrastructure.mapper;

import java.util.List;
import mx.uam.sapcyti.planning.application.service.SaveEntriesUseCase.EntryUpdate;
import mx.uam.sapcyti.planning.infrastructure.adapter.in.dto.SaveEntriesRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AnnualPlanMapper {

    default List<EntryUpdate> toEntryUpdates(SaveEntriesRequest request) {
        return request.entries().stream()
                .map(item -> new EntryUpdate(
                        item.id(),
                        item.gruposI(),
                        item.cupoI(),
                        item.gruposP(),
                        item.cupoP(),
                        item.gruposO(),
                        item.cupoO(),
                        item.marks()))
                .toList();
    }
}

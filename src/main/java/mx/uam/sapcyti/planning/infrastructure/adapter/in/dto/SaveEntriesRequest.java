package mx.uam.sapcyti.planning.infrastructure.adapter.in.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record SaveEntriesRequest(@NotEmpty @Valid List<SaveEntryItem> entries) {

    public record SaveEntryItem(
            @NotNull Long id,
            String gruposI,
            String cupoI,
            String gruposP,
            String cupoP,
            String gruposO,
            String cupoO,
            Map<String, String> marks) {}
}

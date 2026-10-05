package mx.uam.sapcyti.planning.infrastructure.adapter.in.dto;

import java.util.List;
import java.util.Map;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanEntry;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanStatus;

public record AnnualPlanDetailResponse(
        int year,
        AnnualPlanStatus status,
        List<String> terms,
        List<AnnualPlanEntryResponse> entries) {

    public static AnnualPlanDetailResponse from(AnnualPlan plan) {
        return new AnnualPlanDetailResponse(
                plan.getYear(),
                plan.getStatus(),
                AnnualPlan.deriveTerms(plan.getYear()),
                plan.getEntries().stream().map(AnnualPlanEntryResponse::from).toList());
    }

    public record AnnualPlanEntryResponse(
            Long id,
            Long ueaId,
            String clave,
            String nombre,
            String modalidad,
            String gruposI,
            String cupoI,
            String gruposP,
            String cupoP,
            String gruposO,
            String cupoO,
            Map<String, String> marks) {

        static AnnualPlanEntryResponse from(AnnualPlanEntry entry) {
            Map<String, String> marks = entry.getMarks().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            mark -> mark.getKey().name(), Map.Entry::getValue));
            return new AnnualPlanEntryResponse(
                    entry.getId(),
                    entry.getUeaId(),
                    entry.getClave(),
                    entry.getNombre(),
                    entry.getModalidad(),
                    entry.getGruposI(),
                    entry.getCupoI(),
                    entry.getGruposP(),
                    entry.getCupoP(),
                    entry.getGruposO(),
                    entry.getCupoO(),
                    marks);
        }
    }
}

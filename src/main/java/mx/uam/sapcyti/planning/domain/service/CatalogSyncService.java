package mx.uam.sapcyti.planning.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanEntry;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanStatus;
import org.springframework.stereotype.Service;

@Service
public class CatalogSyncService {

    public void sync(AnnualPlan plan, List<UEA> activeUeas) {
        if (plan.getStatus() != AnnualPlanStatus.BORRADOR) {
            return;
        }

        Map<Long, UEA> activeById = activeUeas.stream()
                .collect(Collectors.toMap(UEA::getId, Function.identity()));

        List<AnnualPlanEntry> kept = new ArrayList<>();
        for (AnnualPlanEntry entry : plan.getEntries()) {
            UEA uea = activeById.get(entry.getUeaId());
            if (uea == null) {
                continue;
            }
            entry.refreshSnapshot(uea);
            kept.add(entry);
        }

        Set<Long> keptUeaIds = kept.stream().map(AnnualPlanEntry::getUeaId).collect(Collectors.toSet());
        short nextPosition = (short) (kept.stream()
                        .mapToInt(AnnualPlanEntry::getPosicion)
                        .max()
                        .orElse(0)
                + 1);

        List<UEA> newUeas = activeUeas.stream()
                .filter(uea -> !keptUeaIds.contains(uea.getId()))
                .sorted(Comparator.comparing(UEA::getClave))
                .toList();

        for (UEA uea : newUeas) {
            kept.add(AnnualPlanEntry.createEmpty(plan, uea, nextPosition++));
        }

        plan.replaceEntries(kept);
    }
}

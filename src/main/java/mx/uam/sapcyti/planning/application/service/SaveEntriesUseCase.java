package mx.uam.sapcyti.planning.application.service;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.domain.exception.AnnualPlanNotFoundException;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanEntry;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanChangePort;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveEntriesUseCase {

    private final AnnualPlanRepositoryPort annualPlanRepository;
    private final AnnualPlanChangePort annualPlanChangePort;

    @Transactional
    public AnnualPlan execute(int year, List<EntryUpdate> updates) {
        Long graduateProgramId = requireTenant();
        AnnualPlan plan = annualPlanRepository
                .findByYearAndGraduateProgramId(year, graduateProgramId)
                .orElseThrow(AnnualPlanNotFoundException::new);

        plan.assertEditable();

        Map<Character, Set<Long>> changedUeaIdsByTrimester = new java.util.HashMap<>();
        for (EntryUpdate update : updates) {
            AnnualPlanEntry entry = plan.requireEntry(update.id());
            Set<Character> changedTrimesters = entry.updateValues(
                    update.gruposI(),
                    update.cupoI(),
                    update.gruposP(),
                    update.cupoP(),
                    update.gruposO(),
                    update.cupoO(),
                    toMarkMap(update.marks()));
            for (Character trimester : changedTrimesters) {
                changedUeaIdsByTrimester
                        .computeIfAbsent(trimester, ignored -> new HashSet<>())
                        .add(entry.getUeaId());
            }
        }

        AnnualPlan saved = annualPlanRepository.save(plan);
        if (!changedUeaIdsByTrimester.isEmpty()) {
            annualPlanChangePort.markOutdatedByChangedUeas(
                    year, graduateProgramId, immutableChanges(changedUeaIdsByTrimester));
        }
        return saved;
    }

    private static Map<GraduateProgramMark, String> toMarkMap(Map<String, String> marks) {
        if (marks == null || marks.isEmpty()) {
            return Map.of();
        }
        Map<GraduateProgramMark, String> result = new EnumMap<>(GraduateProgramMark.class);
        for (Map.Entry<String, String> entry : marks.entrySet()) {
            GraduateProgramMark mark = GraduateProgramMark.fromCode(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid value for marks." + entry.getKey() + ": " + entry.getValue()));
            result.put(mark, entry.getValue());
        }
        return result;
    }

    private static Map<Character, Set<Long>> immutableChanges(
            Map<Character, Set<Long>> changedUeaIdsByTrimester) {
        Map<Character, Set<Long>> result = new java.util.HashMap<>();
        changedUeaIdsByTrimester.forEach(
                (trimester, ueaIds) -> result.put(trimester, Set.copyOf(ueaIds)));
        return Map.copyOf(result);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }

    public record EntryUpdate(
            Long id,
            String gruposI,
            String cupoI,
            String gruposP,
            String cupoP,
            String gruposO,
            String cupoO,
            Map<String, String> marks) {}
}

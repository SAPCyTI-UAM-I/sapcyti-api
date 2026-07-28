package mx.uam.sapcyti.planning.domain.port.out;

import java.util.Map;
import java.util.Set;

/**
 * Propagates changes that make existing trimestral plans stale without coupling planning to
 * the trimestral domain model.
 */
public interface AnnualPlanChangePort {

    void markOutdatedByChangedUeas(
            int year,
            Long graduateProgramId,
            Map<Character, Set<Long>> changedUeaIdsByTrimester);

    void markAllOutdatedByYear(int year, Long graduateProgramId);
}

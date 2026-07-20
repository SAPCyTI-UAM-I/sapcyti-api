package mx.uam.sapcyti.survey.domain.port.out;

import java.util.Optional;

/**
 * Gate used by survey reopen (HU-40) to consult trimestral plan status for a term.
 * Implemented by {@code trimestral} (SPEC-035).
 */
public interface TrimestralPlanGatePort {

    Optional<GateStatus> findStatusByTerm(String term, Long graduateProgramId);

    void markOutdatedByTerm(String term, Long graduateProgramId);

    enum GateStatus {
        BORRADOR,
        TERMINADA
    }
}

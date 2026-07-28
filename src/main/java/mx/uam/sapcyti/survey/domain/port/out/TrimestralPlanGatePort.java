package mx.uam.sapcyti.survey.domain.port.out;

/**
 * Port used by survey reopen to make a related trimestral plan stale.
 * Implemented by {@code trimestral} (SPEC-035).
 */
public interface TrimestralPlanGatePort {

    void markOutdatedBySurveyReopened(String term, Long graduateProgramId);
}

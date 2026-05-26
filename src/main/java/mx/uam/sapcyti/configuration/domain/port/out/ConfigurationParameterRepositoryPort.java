package mx.uam.sapcyti.configuration.domain.port.out;

import java.util.List;
import java.util.Optional;

import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;

/**
 * Output port for {@link ConfigurationParameter} persistence.
 *
 * <p><strong>Isolation rule:</strong> all queries MUST include a
 * {@code graduateProgramId} predicate. No queries by key alone are
 * permitted — this enforces multi-tenant isolation (QA-4, SEC-1).
 *
 * @see mx.uam.sapcyti.configuration.infrastructure.adapter.out.ConfigurationParameterJpaAdapter
 */
public interface ConfigurationParameterRepositoryPort {

    /**
     * Persists a new or updated configuration parameter.
     *
     * @param parameter the parameter to save
     * @return the saved parameter with generated id
     */
    ConfigurationParameter save(ConfigurationParameter parameter);

    /**
     * Finds a configuration parameter by program id and key.
     *
     * @param graduateProgramId the program id
     * @param key               the parameter key (UPPER_SNAKE_CASE)
     * @return the parameter, or empty if not found
     */
    Optional<ConfigurationParameter> findByGraduateProgramIdAndKey(
        Long graduateProgramId, String key);

    /**
     * Returns all configuration parameters for a graduate program.
     *
     * @param graduateProgramId the program id
     * @return list of parameters for the program
     */
    List<ConfigurationParameter> findAllByGraduateProgramId(
        Long graduateProgramId);

    /**
     * Deletes a configuration parameter by program id and key.
     *
     * @param graduateProgramId the program id
     * @param key               the parameter key
     */
    void deleteByGraduateProgramIdAndKey(
        Long graduateProgramId, String key);

    /**
     * Checks whether a parameter with the given key exists for a program.
     *
     * @param graduateProgramId the program id
     * @param key               the parameter key
     * @return true if the parameter exists
     */
    boolean existsByGraduateProgramIdAndKey(
        Long graduateProgramId, String key);

    /**
     * Counts configuration parameters for a graduate program.
     *
     * @param graduateProgramId the program id
     * @return number of parameters for the program
     */
    long countByGraduateProgramId(Long graduateProgramId);
}

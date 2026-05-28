package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-based adapter that implements {@link ConfigurationParameterRepositoryPort}.
 *
 * <p><strong>Isolation rule (QA-4):</strong> every query includes an explicit
 * {@code graduateProgramId} predicate so that parameters from one program are
 * never visible to another.
 */
@Repository
public class ConfigurationParameterJpaAdapter
        implements ConfigurationParameterRepositoryPort {

    private final SpringDataConfigParamRepository jpaRepository;

    public ConfigurationParameterJpaAdapter(
            SpringDataConfigParamRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public ConfigurationParameter save(ConfigurationParameter parameter) {
        return jpaRepository.save(parameter);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfigurationParameter> findByGraduateProgramIdAndKey(
            Long graduateProgramId, String key) {
        return jpaRepository
                .findByProgramIdAndKey(graduateProgramId, key);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfigurationParameter> findAllByGraduateProgramId(
            Long graduateProgramId) {
        return jpaRepository.findAllByProgramId(graduateProgramId);
    }

    @Override
    @Transactional
    public void deleteByGraduateProgramIdAndKey(
            Long graduateProgramId, String key) {
        jpaRepository.deleteByProgramIdAndKey(graduateProgramId, key);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByGraduateProgramIdAndKey(
            Long graduateProgramId, String key) {
        return jpaRepository
                .existsByProgramIdAndKey(graduateProgramId, key);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByGraduateProgramId(Long graduateProgramId) {
        return jpaRepository.countByProgramId(graduateProgramId);
    }
}

package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;

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

    private final SpringDataConfigurationParameterRepository jpaRepository;

    public ConfigurationParameterJpaAdapter(
            SpringDataConfigurationParameterRepository jpaRepository) {
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
            .findByGraduateProgram_IdAndKey(graduateProgramId, key);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfigurationParameter> findAllByGraduateProgramId(
            Long graduateProgramId) {
        return jpaRepository
            .findAllByGraduateProgram_Id(graduateProgramId);
    }

    @Override
    @Transactional
    public void deleteByGraduateProgramIdAndKey(
            Long graduateProgramId, String key) {
        jpaRepository
            .deleteByGraduateProgram_IdAndKey(graduateProgramId, key);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByGraduateProgramIdAndKey(
            Long graduateProgramId, String key) {
        return jpaRepository
            .existsByGraduateProgram_IdAndKey(graduateProgramId, key);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByGraduateProgramId(Long graduateProgramId) {
        return jpaRepository.countByGraduateProgram_Id(graduateProgramId);
    }
}

/**
 * Spring Data repository — internal to the infrastructure layer.
 * Not exposed to the domain; only used by
 * {@link ConfigurationParameterJpaAdapter}.
 */
interface SpringDataConfigurationParameterRepository
        extends JpaRepository<ConfigurationParameter, Long> {

    Optional<ConfigurationParameter> findByGraduateProgram_IdAndKey(
        Long graduateProgramId, String key);

    List<ConfigurationParameter> findAllByGraduateProgram_Id(
        Long graduateProgramId);

    @Modifying
    @Query("DELETE FROM ConfigurationParameter cp "
         + "WHERE cp.graduateProgram.id = :programId "
         + "AND cp.key = :key")
    void deleteByGraduateProgram_IdAndKey(
        @Param("programId") Long graduateProgramId,
        @Param("key") String key);

    boolean existsByGraduateProgram_IdAndKey(
        Long graduateProgramId, String key);

    long countByGraduateProgram_Id(Long graduateProgramId);
}

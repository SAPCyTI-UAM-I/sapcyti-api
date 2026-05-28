package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link ConfigurationParameter}.
 *
 * <p>Internal to the infrastructure layer. Not exposed to the domain;
 * only used by {@link ConfigurationParameterJpaAdapter}.
 *
 * <p>All queries use explicit JPQL with {@code graduateProgram.id} predicate
 * to enforce multi-tenant isolation (QA-4) and to avoid Spring Data derived
 * query method names with underscores (Checkstyle MethodName rule).
 */
interface SpringDataConfigParamRepository
        extends JpaRepository<ConfigurationParameter, Long> {

    @Query("SELECT cp FROM ConfigurationParameter cp "
            + "WHERE cp.graduateProgram.id = :programId "
            + "AND cp.key = :key")
    Optional<ConfigurationParameter> findByProgramIdAndKey(
            @Param("programId") Long graduateProgramId,
            @Param("key") String key);

    @Query("SELECT cp FROM ConfigurationParameter cp "
            + "WHERE cp.graduateProgram.id = :programId")
    List<ConfigurationParameter> findAllByProgramId(
            @Param("programId") Long graduateProgramId);

    @Modifying
    @Query("DELETE FROM ConfigurationParameter cp "
            + "WHERE cp.graduateProgram.id = :programId "
            + "AND cp.key = :key")
    void deleteByProgramIdAndKey(
            @Param("programId") Long graduateProgramId,
            @Param("key") String key);

    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END "
            + "FROM ConfigurationParameter cp "
            + "WHERE cp.graduateProgram.id = :programId "
            + "AND cp.key = :key")
    boolean existsByProgramIdAndKey(
            @Param("programId") Long graduateProgramId,
            @Param("key") String key);

    @Query("SELECT COUNT(cp) FROM ConfigurationParameter cp "
            + "WHERE cp.graduateProgram.id = :programId")
    long countByProgramId(@Param("programId") Long graduateProgramId);
}

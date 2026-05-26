package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;

/**
 * JPA-based adapter that implements {@link GraduateProgramRepositoryPort}.
 *
 * <p>Hexagonal boundary preserved: this adapter translates between the domain
 * port contract and Spring Data JPA infrastructure.
 *
 * <p>No tenant filtering is applied — {@code GraduateProgram.id} <em>is</em>
 * the tenant discriminator for downstream tables (QA-4).
 */
@Repository
public class GraduateProgramJpaAdapter implements GraduateProgramRepositoryPort {

    private final SpringDataGraduateProgramRepository jpaRepository;

    public GraduateProgramJpaAdapter(
            SpringDataGraduateProgramRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public GraduateProgram save(GraduateProgram program) {
        return jpaRepository.save(program);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GraduateProgram> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GraduateProgram> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameAndIdNot(String name, Long excludeId) {
        return jpaRepository.existsByNameAndIdNot(name, excludeId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}

/**
 * Spring Data repository — internal to the infrastructure layer.
 * Not exposed to the domain; only used by {@link GraduateProgramJpaAdapter}.
 */
interface SpringDataGraduateProgramRepository
        extends JpaRepository<GraduateProgram, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}

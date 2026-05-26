package mx.uam.sapcyti.configuration.infrastructure.adapter.out;

import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link GraduateProgram}.
 *
 * <p>Internal to the infrastructure layer. Not exposed to the domain;
 * only used by {@link GraduateProgramJpaAdapter}.
 */
interface SpringDataGraduateProgramRepository
        extends JpaRepository<GraduateProgram, Long> {

    boolean existsByName(String name);
}

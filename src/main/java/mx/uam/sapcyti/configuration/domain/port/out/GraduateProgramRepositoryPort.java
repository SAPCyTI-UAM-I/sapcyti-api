package mx.uam.sapcyti.configuration.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;

/**
 * Output port for {@link GraduateProgram} persistence.
 *
 * <p>Domain defines the contract; infrastructure provides the implementation.
 * No tenant filtering on this entity — the program id <em>is</em> the tenant
 * discriminator for child tables (QA-4).
 *
 * @see mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter
 */
public interface GraduateProgramRepositoryPort {

    /**
     * Persists a new or updated graduate program.
     *
     * @param program the program to save
     * @return the saved program with generated id
     */
    GraduateProgram save(GraduateProgram program);

    /**
     * Finds a graduate program by its id.
     *
     * @param id the program id
     * @return the program, or empty if not found
     */
    Optional<GraduateProgram> findById(Long id);

    /**
     * Returns all graduate programs.
     *
     * @return list of all programs (≤9 per QA-4)
     */
    List<GraduateProgram> findAll();

    /**
     * Checks whether a program with the given name already exists.
     *
     * @param name the program name to check
     * @return true if a program with that name exists
     */
    boolean existsByName(String name);

    /**
     * Checks whether another program (different id) has the given name.
     *
     * @param name      the program name to check
     * @param excludeId the program id to exclude from the check
     * @return true if another program with that name exists
     */
    boolean existsByNameAndIdNot(String name, Long excludeId);

    /**
     * Deletes a graduate program by its id.
     *
     * @param id the program id
     */
    void deleteById(Long id);
}

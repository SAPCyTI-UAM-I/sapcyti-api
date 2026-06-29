package mx.uam.sapcyti.offering.infrastructure.adapter.out.repository;

import java.util.Optional;
import mx.uam.sapcyti.offering.domain.model.UEA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataUeaRepository extends JpaRepository<UEA, Long> {

    boolean existsByClaveIgnoreCaseAndGraduateProgramId(String clave, Long graduateProgramId);

    Optional<UEA> findByClaveIgnoreCaseAndGraduateProgramId(String clave, Long graduateProgramId);

    @Query("""
            SELECT u FROM UEA u
            WHERE u.graduateProgramId = :graduateProgramId
              AND (:search IS NULL OR :search = ''
                   OR LOWER(u.clave) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR u.active = :active)
            """)
    Page<UEA> searchByProgram(
            @Param("graduateProgramId") Long graduateProgramId,
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);
}

package mx.uam.sapcyti.offering.domain.port.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.offering.domain.model.UEA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UeaRepositoryPort {

    UEA save(UEA uea);

    List<UEA> saveAll(List<UEA> ueas);

    boolean existsByClaveAndGraduateProgramId(String clave, Long graduateProgramId);

    Optional<UEA> findByClaveAndGraduateProgramId(String clave, Long graduateProgramId);

    Page<UEA> findByGraduateProgramId(Long graduateProgramId, String search, Boolean active, Pageable pageable);
}

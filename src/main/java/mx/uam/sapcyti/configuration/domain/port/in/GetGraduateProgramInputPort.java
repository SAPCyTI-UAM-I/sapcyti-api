package mx.uam.sapcyti.configuration.domain.port.in;

import java.util.List;
import java.util.Optional;

import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramListItemResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;

/**
 * Input port for querying graduate programs.
 */
public interface GetGraduateProgramInputPort {

    Optional<GraduateProgramResponse> getById(Long id);

    List<GraduateProgramListItemResponse> listAll();
}

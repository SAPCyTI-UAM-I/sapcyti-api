package mx.uam.sapcyti.offering.domain.port.out;

import java.util.Optional;

public interface SurveyActivityPort {

    Optional<String> findActiveSurveyTermIncluding(Long ueaId, Long graduateProgramId);
}

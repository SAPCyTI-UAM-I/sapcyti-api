package mx.uam.sapcyti.configuration.domain.port.in;

import mx.uam.sapcyti.configuration.application.command.UpdateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;

/**
 * Input port for updating a graduate program.
 */
public interface UpdateGraduateProgramInputPort {

    GraduateProgramResponse update(UpdateGraduateProgramCommand command);
}

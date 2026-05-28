package mx.uam.sapcyti.configuration.domain.port.in;

import mx.uam.sapcyti.configuration.application.command.CreateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;

/**
 * Input port for creating a graduate program.
 */
public interface CreateGraduateProgramInputPort {

    GraduateProgramResponse create(CreateGraduateProgramCommand command);
}

package mx.uam.sapcyti.configuration.domain.port.in;

import mx.uam.sapcyti.configuration.application.command.SetConfigurationParameterCommand;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.ConfigurationParameterResponse;

/**
 * Input port for creating or updating a configuration parameter.
 */
public interface SetConfigurationParameterInputPort {

    ConfigurationParameterResponse set(
        Long graduateProgramId,
        SetConfigurationParameterCommand command);
}

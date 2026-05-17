package mx.uam.sapcyti.configuration.domain.port.in;

import java.util.List;
import java.util.Optional;

import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.ConfigurationParameterResponse;

/**
 * Input port for querying configuration parameters.
 */
public interface GetConfigurationParametersInputPort {

    List<ConfigurationParameterResponse> listByProgram(Long graduateProgramId);

    Optional<ConfigurationParameterResponse> getByProgramAndKey(
        Long graduateProgramId,
        String key);
}

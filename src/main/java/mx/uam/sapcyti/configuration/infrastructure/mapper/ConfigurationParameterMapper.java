package mx.uam.sapcyti.configuration.infrastructure.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import mx.uam.sapcyti.configuration.application.command.InitialParameterCommand;
import mx.uam.sapcyti.configuration.application.command.SetConfigurationParameterCommand;
import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.ConfigurationParameterResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.InitialParameterRequest;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.SetConfigurationParameterRequest;

/**
 * MapStruct mapper for configuration parameters.
 */
@Mapper(componentModel = "spring")
public interface ConfigurationParameterMapper {

    ConfigurationParameterResponse toResponse(ConfigurationParameter parameter);

    List<ConfigurationParameterResponse> toResponseList(
        List<ConfigurationParameter> parameters);

    SetConfigurationParameterCommand toSetCommand(
        SetConfigurationParameterRequest request);

    InitialParameterCommand toInitialCommand(InitialParameterRequest request);

    @Mapping(target = "graduateProgram", ignore = true)
    @Mapping(target = "id", ignore = true)
    ConfigurationParameter toEntity(InitialParameterCommand command);
}

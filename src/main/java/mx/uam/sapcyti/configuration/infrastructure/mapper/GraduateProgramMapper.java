package mx.uam.sapcyti.configuration.infrastructure.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import mx.uam.sapcyti.configuration.application.command.CreateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.application.command.UpdateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.CreateGraduateProgramRequest;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramListItemResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.UpdateGraduateProgramRequest;

/**
 * MapStruct mapper for graduate programs.
 */
@Mapper(
    componentModel = "spring",
    uses = ConfigurationParameterMapper.class)
public interface GraduateProgramMapper {

    CreateGraduateProgramCommand toCommand(CreateGraduateProgramRequest request);

    @Mapping(target = "id", source = "programId")
    UpdateGraduateProgramCommand toCommand(
        Long programId,
        UpdateGraduateProgramRequest request);

    @Mapping(target = "configurationParameters", source = "parameters")
    GraduateProgramResponse toResponse(
        GraduateProgram program,
        List<ConfigurationParameter> parameters);

    GraduateProgramListItemResponse toListItem(
        GraduateProgram program,
        long parameterCount);
}

package mx.uam.sapcyti.academic.infrastructure.mapper;

import mx.uam.sapcyti.academic.application.command.RegisterProfessorCommand;
import mx.uam.sapcyti.academic.application.service.ListProfessorsUseCase;
import mx.uam.sapcyti.academic.application.service.RegisterProfessorUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.ProfessorResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterProfessorRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfessorMapper {

    RegisterProfessorCommand toCommand(RegisterProfessorRequest request);

    @Mapping(target = "generatedPassword", source = "generatedPassword")
    @Mapping(target = "active", constant = "true")
    ProfessorResponse toResponse(RegisterProfessorUseCase.RegisterProfessorResult result);

    @Mapping(target = "generatedPassword", ignore = true)
    ProfessorResponse toResponse(ListProfessorsUseCase.ProfessorListItem item);
}

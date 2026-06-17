package mx.uam.sapcyti.academic.infrastructure.mapper;

import mx.uam.sapcyti.academic.application.command.RegisterStudentCommand;
import mx.uam.sapcyti.academic.application.service.ListStudentsUseCase;
import mx.uam.sapcyti.academic.application.service.RegisterStudentUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterStudentRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    RegisterStudentCommand toCommand(RegisterStudentRequest request);

    @Mapping(target = "generatedPassword", source = "generatedPassword")
    @Mapping(target = "active", constant = "true")
    StudentResponse toResponse(RegisterStudentUseCase.RegisterStudentResult result);

    @Mapping(target = "generatedPassword", ignore = true)
    StudentResponse toResponse(ListStudentsUseCase.StudentListItem item);
}

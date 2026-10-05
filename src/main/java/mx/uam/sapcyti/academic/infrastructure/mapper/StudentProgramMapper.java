package mx.uam.sapcyti.academic.infrastructure.mapper;

import mx.uam.sapcyti.academic.application.command.UpdateStudentProgramCommand;
import mx.uam.sapcyti.academic.application.service.GetStudentProgramUseCase;
import mx.uam.sapcyti.academic.application.service.ListStudentProgramsUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.ProfessorReferenceResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentProgramResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentProgramSummaryResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.UpdateStudentProgramRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentProgramMapper {

    StudentProgramSummaryResponse toSummaryResponse(ListStudentProgramsUseCase.StudentProgramSummaryItem item);

    StudentProgramResponse toResponse(GetStudentProgramUseCase.StudentProgramDetail detail);

    ProfessorReferenceResponse toProfessorReference(GetStudentProgramUseCase.ProfessorReference reference);

    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "programId", source = "programId")
    UpdateStudentProgramCommand toCommand(
            Long studentId, Long programId, UpdateStudentProgramRequest request);
}

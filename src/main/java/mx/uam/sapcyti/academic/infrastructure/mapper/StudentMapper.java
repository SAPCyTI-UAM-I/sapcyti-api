package mx.uam.sapcyti.academic.infrastructure.mapper;

import mx.uam.sapcyti.academic.application.command.RegisterStudentCommand;
import mx.uam.sapcyti.academic.application.command.UpdateStudentCommand;
import mx.uam.sapcyti.academic.application.service.GetStudentDetailUseCase;
import mx.uam.sapcyti.academic.application.service.ListStudentsUseCase;
import mx.uam.sapcyti.academic.application.service.RegisterStudentUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterStudentRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentDetailResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.UpdateStudentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = StudentProgramMapper.class)
public interface StudentMapper {

    RegisterStudentCommand toCommand(RegisterStudentRequest request);

    @Mapping(target = "generatedPassword", source = "generatedPassword")
    @Mapping(target = "active", constant = "true")
    StudentResponse toResponse(RegisterStudentUseCase.RegisterStudentResult result);

    @Mapping(target = "generatedPassword", ignore = true)
    StudentResponse toResponse(ListStudentsUseCase.StudentListItem item);

    @Mapping(target = "studentId", source = "studentId")
    UpdateStudentCommand toCommand(Long studentId, UpdateStudentRequest request);

    @Mapping(target = "id", source = "student.id")
    @Mapping(target = "enrollmentId", source = "student.enrollmentId")
    @Mapping(target = "email", source = "student.email")
    @Mapping(target = "firstName", source = "student.firstName")
    @Mapping(target = "firstLastName", source = "student.firstLastName")
    @Mapping(target = "secondLastName", source = "student.secondLastName")
    @Mapping(target = "nationality", source = "student.nationality")
    @Mapping(target = "birthDate", source = "student.birthDate")
    @Mapping(target = "phone", source = "student.phone")
    @Mapping(target = "phoneExtension", source = "student.phoneExtension")
    @Mapping(target = "undergraduateDegree", source = "student.undergraduateDegree")
    @Mapping(target = "lastDegreeObtained", source = "student.lastDegreeObtained")
    @Mapping(target = "programType", source = "student.programType")
    @Mapping(target = "admissionDate", source = "student.admissionDate")
    @Mapping(target = "admissionTerm", source = "student.admissionTerm")
    @Mapping(target = "advisorId", source = "student.advisorId")
    @Mapping(target = "graduateProgramId", source = "student.graduateProgramId")
    @Mapping(target = "userId", source = "student.userId")
    @Mapping(target = "active", source = "student.active")
    @Mapping(target = "program", source = "program")
    StudentDetailResponse toDetailResponse(GetStudentDetailUseCase.StudentDetail detail);
}

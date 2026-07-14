package mx.uam.sapcyti.survey.infrastructure.mapper;

import java.time.Instant;
import mx.uam.sapcyti.survey.application.command.CreateSurveyCommand;
import mx.uam.sapcyti.survey.application.command.SubmitResponseCommand;
import mx.uam.sapcyti.survey.application.command.UpdateSurveyCommand;
import mx.uam.sapcyti.survey.application.service.InterestedStudentView;
import mx.uam.sapcyti.survey.application.service.StudentSurveyForm;
import mx.uam.sapcyti.survey.application.service.SubmittedResponseView;
import mx.uam.sapcyti.survey.application.service.SurveyDetail;
import mx.uam.sapcyti.survey.application.service.SurveyResultsSummary;
import mx.uam.sapcyti.survey.application.service.UeaDemandRow;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.CreateSurveyRequest;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.EnrollmentSurveyResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.InterestedStudentResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.StudentSurveyFormResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.SubmitResponseRequest;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.SubmittedResponseDto;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.SurveyResultsSummaryResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.UeaDemandRowResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.UpdateSurveyRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentSurveyMapper {

    CreateSurveyCommand toCommand(CreateSurveyRequest request);

    UpdateSurveyCommand toCommand(UpdateSurveyRequest request);

    SubmitResponseCommand toCommand(SubmitResponseRequest request);

    @Mapping(target = "status", expression = "java(detail.getSurvey().getStatus(java.time.Instant.now()))")
    @Mapping(target = "id", source = "survey.id")
    @Mapping(target = "term", source = "survey.term")
    @Mapping(target = "opensAt", source = "survey.opensAt")
    @Mapping(target = "closesAt", source = "survey.closesAt")
    @Mapping(target = "introMessage", source = "survey.introMessage")
    EnrollmentSurveyResponse toResponse(SurveyDetail detail);

    SubmittedResponseDto toResponse(SubmittedResponseView view);

    SurveyResultsSummaryResponse toResponse(SurveyResultsSummary summary);

    UeaDemandRowResponse toResponse(UeaDemandRow row);

    InterestedStudentResponse toResponse(InterestedStudentView view);

    @Mapping(target = "survey", source = "survey")
    @Mapping(target = "student", source = "student")
    @Mapping(target = "availableUeas", source = "availableUeas")
    StudentSurveyFormResponse toResponse(StudentSurveyForm form);

    StudentSurveyFormResponse.StudentInfoDto toStudentInfo(StudentSurveyForm.StudentInfo student);

    StudentSurveyFormResponse.AvailableUeaDto toAvailableUea(StudentSurveyForm.UeaSummary uea);
}

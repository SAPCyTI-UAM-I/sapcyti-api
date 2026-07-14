package mx.uam.sapcyti.survey.infrastructure.adapter.in.dto;

import java.util.List;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

public record StudentSurveyFormResponse(
        EnrollmentSurveyResponse survey,
        StudentInfoDto student,
        List<AvailableUeaDto> availableUeas,
        List<String> removedUeaClaves,
        SubmittedResponseDto myResponse) {

    public record StudentInfoDto(String fullName, String enrollmentId, ProgramType programType) {}

    public record AvailableUeaDto(Long id, String clave, String nombre, int creditos) {}
}

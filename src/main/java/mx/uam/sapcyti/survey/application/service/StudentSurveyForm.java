package mx.uam.sapcyti.survey.application.service;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.offering.domain.model.UEA;

@Value
@Builder
public class StudentSurveyForm {

    SurveyDetail survey;
    StudentInfo student;
    List<UeaSummary> availableUeas;
    List<String> removedUeaClaves;
    SubmittedResponseView myResponse;

    @Value
    @Builder
    public static class StudentInfo {
        String fullName;
        String enrollmentId;
        ProgramType programType;
    }

    @Value
    @Builder
    public static class UeaSummary {
        Long id;
        String clave;
        String nombre;
        int creditos;

        public static UeaSummary from(UEA uea) {
            return UeaSummary.builder()
                    .id(uea.getId())
                    .clave(uea.getClave())
                    .nombre(uea.getNombre())
                    .creditos(uea.getCreditos())
                    .build();
        }
    }
}

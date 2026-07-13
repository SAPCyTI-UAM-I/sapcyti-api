package mx.uam.sapcyti.survey.application.service;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterestedStudentView {
    String fullName;
    String enrollmentId;
}

package mx.uam.sapcyti.survey.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.identity.infrastructure.security.AuthenticatedUserResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentSurveyContextResolver {

    private final StudentRepositoryPort studentRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public Student requireCurrentStudent() {
        Long graduateProgramId = SurveyDetailFactory.requireTenant();
        Long userId = authenticatedUserResolver.resolve().getUserId();
        return studentRepository.findByUserIdAndGraduateProgramId(userId, graduateProgramId)
                .orElseThrow(StudentNotFoundException::new);
    }
}

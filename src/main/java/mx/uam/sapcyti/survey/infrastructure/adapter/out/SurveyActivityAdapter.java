package mx.uam.sapcyti.survey.infrastructure.adapter.out;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.port.out.SurveyActivityPort;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SurveyActivityAdapter implements SurveyActivityPort {

    private final EnrollmentSurveyRepositoryPort surveyRepository;

    @Override
    public Optional<String> findActiveSurveyTermIncluding(Long ueaId, Long graduateProgramId) {
        return surveyRepository.findActiveSurveyTermIncludingUea(
                ueaId, graduateProgramId, Instant.now());
    }
}

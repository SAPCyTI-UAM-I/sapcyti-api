package mx.uam.sapcyti.survey.domain.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;

public interface EnrollmentSurveyRepositoryPort {

    EnrollmentSurvey save(EnrollmentSurvey survey);

    void delete(EnrollmentSurvey survey);

    Optional<EnrollmentSurvey> findByIdAndGraduateProgramId(Long id, Long graduateProgramId);

    List<EnrollmentSurvey> findAllByGraduateProgramIdOrderByCreatedAtDesc(Long graduateProgramId);

    boolean existsByTermAndGraduateProgramId(String term, Long graduateProgramId);

    boolean existsWindowOverlap(Long graduateProgramId, Instant opensAt, Instant closesAt, Long excludeSurveyId);

    Optional<EnrollmentSurvey> findLatestByGraduateProgramId(Long graduateProgramId);

    Optional<EnrollmentSurvey> findActiveByGraduateProgramId(Long graduateProgramId, Instant now);

    Optional<String> findActiveSurveyTermIncludingUea(Long ueaId, Long graduateProgramId, Instant now);

    long countResponsesBySurveyId(Long surveyId);
}

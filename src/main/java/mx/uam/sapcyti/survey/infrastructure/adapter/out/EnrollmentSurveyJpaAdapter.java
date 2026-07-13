package mx.uam.sapcyti.survey.infrastructure.adapter.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.survey.infrastructure.adapter.out.repository.SpringDataEnrollmentSurveyRepository;
import mx.uam.sapcyti.survey.infrastructure.adapter.out.repository.SpringDataSurveyResponseRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class EnrollmentSurveyJpaAdapter implements EnrollmentSurveyRepositoryPort {

    private final SpringDataEnrollmentSurveyRepository jpaRepository;
    private final SpringDataSurveyResponseRepository responseRepository;

    @Override
    @Transactional
    public EnrollmentSurvey save(EnrollmentSurvey survey) {
        return jpaRepository.save(survey);
    }

    @Override
    @Transactional
    public void delete(EnrollmentSurvey survey) {
        jpaRepository.delete(survey);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnrollmentSurvey> findByIdAndGraduateProgramId(Long id, Long graduateProgramId) {
        return jpaRepository.findByIdAndGraduateProgramId(id, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentSurvey> findAllByGraduateProgramIdOrderByCreatedAtDesc(Long graduateProgramId) {
        return jpaRepository.findAllByGraduateProgramIdOrderByCreatedAtDesc(graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTermAndGraduateProgramId(String term, Long graduateProgramId) {
        return jpaRepository.existsByTermAndGraduateProgramId(term, graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnrollmentSurvey> findLatestByGraduateProgramId(Long graduateProgramId) {
        return jpaRepository.findFirstByGraduateProgramIdOrderByCreatedAtDesc(graduateProgramId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnrollmentSurvey> findActiveByGraduateProgramId(Long graduateProgramId, Instant now) {
        return jpaRepository.findActiveByGraduateProgramId(graduateProgramId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findActiveSurveyTermIncludingUea(Long ueaId, Long graduateProgramId, Instant now) {
        return jpaRepository.findActiveSurveyTermIncludingUea(ueaId, graduateProgramId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public long countResponsesBySurveyId(Long surveyId) {
        return responseRepository.countBySurveyId(surveyId);
    }
}

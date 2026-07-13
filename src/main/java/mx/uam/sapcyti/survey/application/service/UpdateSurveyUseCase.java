package mx.uam.sapcyti.survey.application.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.application.command.UpdateSurveyCommand;
import mx.uam.sapcyti.survey.domain.exception.SurveyNoActiveUeasException;
import mx.uam.sapcyti.survey.domain.exception.SurveyWindowOverlapException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateSurveyUseCase {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final UeaRepositoryPort ueaRepository;
    private final SurveyDetailFactory surveyDetailFactory;

    @Transactional
    public SurveyDetail execute(Long surveyId, UpdateSurveyCommand command) {
        EnrollmentSurvey survey = surveyDetailFactory.requireSurvey(surveyId);
        SurveyStatus currentStatus = survey.getStatus(Instant.now());

        if (surveyRepository.existsWindowOverlap(
                survey.getGraduateProgramId(), command.opensAt(), command.closesAt(), surveyId)) {
            throw new SurveyWindowOverlapException();
        }

        if (currentStatus == SurveyStatus.CERRADO) {
            Instant now = Instant.now();
            if (!command.opensAt().isAfter(now) || !command.closesAt().isAfter(now)) {
                throw new IllegalArgumentException("Reopen requires future opensAt and closesAt");
            }
            List<Long> snapshotUeaIds = ueaRepository.findActiveByGraduateProgramId(survey.getGraduateProgramId())
                    .stream()
                    .map(UEA::getId)
                    .toList();
            if (snapshotUeaIds.isEmpty()) {
                throw new SurveyNoActiveUeasException();
            }
            survey.reopen(command.opensAt(), command.closesAt(), command.introMessage(), snapshotUeaIds);
        } else {
            survey.updateSchedule(command.opensAt(), command.closesAt(), command.introMessage());
        }

        return surveyDetailFactory.toDetail(surveyRepository.save(survey));
    }
}

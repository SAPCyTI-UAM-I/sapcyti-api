package mx.uam.sapcyti.survey.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.application.command.UpdateSurveyCommand;
import mx.uam.sapcyti.survey.domain.exception.SurveyNoActiveUeasException;
import mx.uam.sapcyti.survey.domain.exception.SurveyReopenBlockedByFinalPlanException;
import mx.uam.sapcyti.survey.domain.exception.SurveyReopenDatesInvalidException;
import mx.uam.sapcyti.survey.domain.exception.SurveyWindowOverlapException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.survey.domain.port.out.TrimestralPlanGatePort;
import mx.uam.sapcyti.survey.domain.port.out.TrimestralPlanGatePort.GateStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateSurveyUseCase {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final UeaRepositoryPort ueaRepository;
    private final SurveyDetailFactory surveyDetailFactory;
    private final TrimestralPlanGatePort trimestralPlanGate;

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
            // Only the closing date must be in the future — keeping the original
            // (past) opening date reopens the survey as immediately ACTIVO.
            if (!command.closesAt().isAfter(now)) {
                throw new SurveyReopenDatesInvalidException();
            }
            List<Long> snapshotUeaIds = ueaRepository.findActiveByGraduateProgramId(survey.getGraduateProgramId())
                    .stream()
                    .map(UEA::getId)
                    .toList();
            if (snapshotUeaIds.isEmpty()) {
                throw new SurveyNoActiveUeasException();
            }

            Optional<GateStatus> planStatus =
                    trimestralPlanGate.findStatusByTerm(survey.getTerm(), survey.getGraduateProgramId());
            if (planStatus.isPresent() && planStatus.get() == GateStatus.TERMINADA) {
                throw new SurveyReopenBlockedByFinalPlanException();
            }

            survey.reopen(command.opensAt(), command.closesAt(), command.introMessage(), snapshotUeaIds);
            EnrollmentSurvey saved = surveyRepository.save(survey);
            if (planStatus.isPresent() && planStatus.get() == GateStatus.BORRADOR) {
                trimestralPlanGate.markOutdatedByTerm(survey.getTerm(), survey.getGraduateProgramId());
            }
            return surveyDetailFactory.toDetail(saved);
        }

        survey.updateSchedule(command.opensAt(), command.closesAt(), command.introMessage());
        return surveyDetailFactory.toDetail(surveyRepository.save(survey));
    }
}

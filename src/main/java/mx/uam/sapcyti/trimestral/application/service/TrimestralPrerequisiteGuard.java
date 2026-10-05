package mx.uam.sapcyti.trimestral.application.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanStatus;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotClosedException;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotFoundException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.trimestral.domain.exception.AnnualPlanNotTerminatedException;
import mx.uam.sapcyti.trimestral.domain.exception.AnnualPlanRequiredException;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrimestralPrerequisiteGuard {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final AnnualPlanRepositoryPort annualPlanRepository;

    public Prerequisites evaluate(TrimestralPlan plan) {
        EnrollmentSurvey survey = surveyRepository
                .findByIdAndGraduateProgramId(plan.getSurveyId(), plan.getGraduateProgramId())
                .orElse(null);
        boolean surveyClosed =
                survey != null && survey.getStatus(Instant.now()) == SurveyStatus.CERRADO;
        boolean annualPlanTerminated = annualPlanRepository
                .findByYearAndGraduateProgramId(
                        TrimestralPlan.yearFromTerm(plan.getTerm()), plan.getGraduateProgramId())
                .map(annual -> annual.getStatus() == AnnualPlanStatus.TERMINADA)
                .orElse(false);
        return new Prerequisites(surveyClosed, annualPlanTerminated);
    }

    public void assertSatisfied(TrimestralPlan plan) {
        EnrollmentSurvey survey = surveyRepository
                .findByIdAndGraduateProgramId(plan.getSurveyId(), plan.getGraduateProgramId())
                .orElseThrow(SurveyNotFoundException::new);
        assertSatisfied(survey, plan.getGraduateProgramId());
    }

    public void assertSatisfied(EnrollmentSurvey survey, Long graduateProgramId) {
        if (survey.getStatus(Instant.now()) != SurveyStatus.CERRADO) {
            throw new SurveyNotClosedException();
        }
        var annualPlan = annualPlanRepository
                .findByYearAndGraduateProgramId(
                        TrimestralPlan.yearFromTerm(survey.getTerm()), graduateProgramId)
                .orElseThrow(AnnualPlanRequiredException::new);
        if (annualPlan.getStatus() != AnnualPlanStatus.TERMINADA) {
            throw new AnnualPlanNotTerminatedException();
        }
    }

    public record Prerequisites(boolean surveyClosed, boolean annualPlanTerminated) {}
}

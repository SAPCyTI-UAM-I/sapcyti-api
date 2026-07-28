package mx.uam.sapcyti.survey.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.application.command.UpdateSurveyCommand;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.survey.domain.port.out.TrimestralPlanGatePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateSurveyUseCaseTest {

    @Mock private EnrollmentSurveyRepositoryPort surveyRepository;
    @Mock private UeaRepositoryPort ueaRepository;
    @Mock private SurveyDetailFactory surveyDetailFactory;
    @Mock private TrimestralPlanGatePort trimestralPlanGate;

    @Test
    @DisplayName("reopens a closed survey and marks any related plan outdated")
    void reopenAlwaysPropagatesOutdatedReason() {
        UpdateSurveyUseCase useCase = new UpdateSurveyUseCase(
                surveyRepository, ueaRepository, surveyDetailFactory, trimestralPlanGate);
        EnrollmentSurvey survey = mock(EnrollmentSurvey.class);
        UEA uea = mock(UEA.class);
        SurveyDetail detail = mock(SurveyDetail.class);
        Instant opensAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant closesAt = Instant.now().plus(10, ChronoUnit.DAYS);
        UpdateSurveyCommand command =
                new UpdateSurveyCommand("26O", opensAt, closesAt, "Reabierta");
        when(surveyDetailFactory.requireSurvey(12L)).thenReturn(survey);
        when(survey.getStatus(org.mockito.ArgumentMatchers.any())).thenReturn(SurveyStatus.CERRADO);
        when(survey.getGraduateProgramId()).thenReturn(7L);
        when(survey.getTerm()).thenReturn("26O");
        when(surveyRepository.existsWindowOverlap(7L, opensAt, closesAt, 12L))
                .thenReturn(false);
        when(uea.getId()).thenReturn(50L);
        when(ueaRepository.findActiveByGraduateProgramId(7L)).thenReturn(List.of(uea));
        when(surveyRepository.save(survey)).thenReturn(survey);
        when(surveyDetailFactory.toDetail(survey)).thenReturn(detail);

        SurveyDetail result = useCase.execute(12L, command);

        assertThat(result).isSameAs(detail);
        verify(survey).reopen(opensAt, closesAt, "Reabierta", List.of(50L));
        verify(trimestralPlanGate).markOutdatedBySurveyReopened("26O", 7L);
    }
}

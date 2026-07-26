package mx.uam.sapcyti.planning.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanStatus;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanChangePort;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChangeStatusUseCaseTest {

    @Mock private AnnualPlanRepositoryPort annualPlanRepository;
    @Mock private AnnualPlanChangePort annualPlanChangePort;

    private ChangeStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeStatusUseCase(annualPlanRepository, annualPlanChangePort);
        TenantContext.set(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("marks every trimester outdated when annual plan leaves TERMINADA")
    void leavingTerminatedInvalidatesAllTrimesters() {
        AnnualPlan plan = mock(AnnualPlan.class);
        when(plan.getStatus()).thenReturn(AnnualPlanStatus.TERMINADA);
        when(annualPlanRepository.findByYearAndGraduateProgramId(2027, 7L))
                .thenReturn(Optional.of(plan));
        when(annualPlanRepository.save(plan)).thenReturn(plan);

        AnnualPlan result = useCase.execute(2027, AnnualPlanStatus.BORRADOR);

        assertThat(result).isSameAs(plan);
        verify(plan).transitionTo(AnnualPlanStatus.BORRADOR);
        verify(annualPlanChangePort).markAllOutdatedByYear(2027, 7L);
    }

    @Test
    @DisplayName("terminating an annual plan does not create an outdated reason")
    void terminatingDoesNotInvalidateTrimesters() {
        AnnualPlan plan = mock(AnnualPlan.class);
        when(plan.getStatus()).thenReturn(AnnualPlanStatus.BORRADOR);
        when(annualPlanRepository.findByYearAndGraduateProgramId(2027, 7L))
                .thenReturn(Optional.of(plan));
        when(annualPlanRepository.save(plan)).thenReturn(plan);

        useCase.execute(2027, AnnualPlanStatus.TERMINADA);

        verifyNoInteractions(annualPlanChangePort);
    }
}

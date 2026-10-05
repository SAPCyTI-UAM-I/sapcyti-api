package mx.uam.sapcyti.planning.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mx.uam.sapcyti.planning.application.service.SaveEntriesUseCase.EntryUpdate;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanEntry;
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
class SaveEntriesUseCaseTest {

    @Mock private AnnualPlanRepositoryPort annualPlanRepository;
    @Mock private AnnualPlanChangePort annualPlanChangePort;

    private SaveEntriesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SaveEntriesUseCase(annualPlanRepository, annualPlanChangePort);
        TenantContext.set(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("propagates only UEAs and trimesters whose quota pairs changed")
    void propagatesChangedQuotaPairs() {
        AnnualPlan plan = mock(AnnualPlan.class);
        AnnualPlanEntry first = mock(AnnualPlanEntry.class);
        AnnualPlanEntry second = mock(AnnualPlanEntry.class);
        when(annualPlanRepository.findByYearAndGraduateProgramId(2027, 7L))
                .thenReturn(Optional.of(plan));
        when(plan.requireEntry(10L)).thenReturn(first);
        when(plan.requireEntry(11L)).thenReturn(second);
        when(first.getUeaId()).thenReturn(100L);
        when(second.getUeaId()).thenReturn(101L);
        when(first.updateValues("2", "15", null, null, null, null, Map.of()))
                .thenReturn(Set.of('I'));
        when(second.updateValues(null, null, "*", "20", "*", "*", Map.of()))
                .thenReturn(Set.of('P', 'O'));
        when(annualPlanRepository.save(plan)).thenReturn(plan);

        AnnualPlan result = useCase.execute(2027, List.of(
                update(10L, "2", "15", null, null, null, null),
                update(11L, null, null, "*", "20", "*", "*")));

        assertThat(result).isSameAs(plan);
        verify(annualPlanChangePort).markOutdatedByChangedUeas(
                2027,
                7L,
                Map.of('I', Set.of(100L), 'P', Set.of(101L), 'O', Set.of(101L)));
    }

    @Test
    @DisplayName("does not propagate when only marks or identical quota values are saved")
    void noQuotaChanges() {
        AnnualPlan plan = mock(AnnualPlan.class);
        AnnualPlanEntry entry = mock(AnnualPlanEntry.class);
        when(annualPlanRepository.findByYearAndGraduateProgramId(2027, 7L))
                .thenReturn(Optional.of(plan));
        when(plan.requireEntry(10L)).thenReturn(entry);
        when(entry.updateValues(null, null, null, null, null, null, Map.of()))
                .thenReturn(Set.of());
        when(annualPlanRepository.save(plan)).thenReturn(plan);

        useCase.execute(2027, List.of(update(10L, null, null, null, null, null, null)));

        verifyNoInteractions(annualPlanChangePort);
    }

    private static EntryUpdate update(
            Long id,
            String gruposI,
            String cupoI,
            String gruposP,
            String cupoP,
            String gruposO,
            String cupoO) {
        return new EntryUpdate(
                id, gruposI, cupoI, gruposP, cupoP, gruposO, cupoO, Map.of());
    }
}

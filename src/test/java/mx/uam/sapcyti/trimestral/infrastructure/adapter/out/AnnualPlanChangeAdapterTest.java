package mx.uam.sapcyti.trimestral.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import mx.uam.sapcyti.trimestral.domain.model.OutdatedReason;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnualPlanChangeAdapterTest {

    @Mock
    private TrimestralPlanRepositoryPort repository;

    @Test
    void marksOnlyTrimesterWhoseAnnualPairsChanged() {
        TrimestralPlan i = TrimestralPlan.create(1L, 1L, "26I", 5L);
        TrimestralPlan p = TrimestralPlan.create(1L, 2L, "26P", 5L);
        TrimestralPlan o = TrimestralPlan.create(1L, 3L, "26O", 5L);
        when(repository.findAllByGraduateProgramId(1L)).thenReturn(List.of(i, p, o));
        AnnualPlanChangeAdapter adapter = new AnnualPlanChangeAdapter(repository);

        adapter.markOutdatedByChangedUeas(
                2026, 1L, Map.of('I', Set.of(10L), 'P', Set.of(), 'O', Set.of()));

        assertThat(i.getOutdatedReasons()).containsExactly(OutdatedReason.ANNUAL_PLAN_CHANGED);
        assertThat(p.getOutdatedReasons()).isEmpty();
        assertThat(o.getOutdatedReasons()).isEmpty();
        verify(repository).save(i);
        verify(repository, never()).save(p);
        verify(repository, never()).save(o);
    }

    @Test
    void leavingAnnualTerminadaMarksAllPlansInThatYear() {
        TrimestralPlan i = TrimestralPlan.create(1L, 1L, "26I", 5L);
        TrimestralPlan o = TrimestralPlan.create(1L, 2L, "26O", 5L);
        TrimestralPlan anotherYear = TrimestralPlan.create(1L, 3L, "27I", 5L);
        when(repository.findAllByGraduateProgramId(1L))
                .thenReturn(List.of(i, o, anotherYear));
        AnnualPlanChangeAdapter adapter = new AnnualPlanChangeAdapter(repository);

        adapter.markAllOutdatedByYear(2026, 1L);

        assertThat(i.isOutdated()).isTrue();
        assertThat(o.isOutdated()).isTrue();
        assertThat(anotherYear.isOutdated()).isFalse();
        verify(repository).save(i);
        verify(repository).save(o);
        verify(repository, never()).save(anotherYear);
    }
}

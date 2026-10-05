package mx.uam.sapcyti.planning.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.uam.sapcyti.planning.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnnualPlanTest {

    @Test
    @DisplayName("allows adjacent status transitions")
    void adjacentTransitions() {
        AnnualPlan plan = planWithStatus(AnnualPlanStatus.BORRADOR);

        plan.transitionTo(AnnualPlanStatus.TERMINADA);
        assertThat(plan.getStatus()).isEqualTo(AnnualPlanStatus.TERMINADA);

        plan.transitionTo(AnnualPlanStatus.BORRADOR);
        assertThat(plan.getStatus()).isEqualTo(AnnualPlanStatus.BORRADOR);

        plan.transitionTo(AnnualPlanStatus.TERMINADA);
        plan.transitionTo(AnnualPlanStatus.ARCHIVADA);
        assertThat(plan.getStatus()).isEqualTo(AnnualPlanStatus.ARCHIVADA);

        plan.transitionTo(AnnualPlanStatus.TERMINADA);
        assertThat(plan.getStatus()).isEqualTo(AnnualPlanStatus.TERMINADA);
    }

    @Test
    @DisplayName("rejects non-adjacent and same-status transitions")
    void invalidTransitions() {
        AnnualPlan plan = planWithStatus(AnnualPlanStatus.BORRADOR);

        assertThatThrownBy(() -> plan.transitionTo(AnnualPlanStatus.ARCHIVADA))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> plan.transitionTo(AnnualPlanStatus.BORRADOR))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("derives trimester labels from year")
    void deriveTerms() {
        assertThat(AnnualPlan.deriveTerms(2027)).containsExactly("27-I", "27-P", "27-O");
    }

    private static AnnualPlan planWithStatus(AnnualPlanStatus status) {
        AnnualPlan plan = AnnualPlan.create(1L, 2027, 1L, java.util.List.of(), java.util.Map.of());
        if (status == AnnualPlanStatus.TERMINADA) {
            plan.transitionTo(AnnualPlanStatus.TERMINADA);
        } else if (status == AnnualPlanStatus.ARCHIVADA) {
            plan.transitionTo(AnnualPlanStatus.TERMINADA);
            plan.transitionTo(AnnualPlanStatus.ARCHIVADA);
        }
        return plan;
    }
}

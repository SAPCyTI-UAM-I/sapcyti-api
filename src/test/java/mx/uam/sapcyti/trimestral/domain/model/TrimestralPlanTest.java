package mx.uam.sapcyti.trimestral.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.uam.sapcyti.trimestral.domain.exception.InvalidTrimestralStatusTransitionException;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotEditableException;
import org.junit.jupiter.api.Test;

class TrimestralPlanTest {

    @Test
    void transitionBorradorToTerminadaAndBack() {
        TrimestralPlan plan = TrimestralPlan.create(1L, 10L, "26O", 5L);
        plan.transitionTo(TrimestralPlanStatus.TERMINADA);
        assertThat(plan.getStatus()).isEqualTo(TrimestralPlanStatus.TERMINADA);
        plan.transitionTo(TrimestralPlanStatus.BORRADOR);
        assertThat(plan.getStatus()).isEqualTo(TrimestralPlanStatus.BORRADOR);
    }

    @Test
    void sameStatusTransitionRejected() {
        TrimestralPlan plan = TrimestralPlan.create(1L, 10L, "26O", 5L);
        assertThatThrownBy(() -> plan.transitionTo(TrimestralPlanStatus.BORRADOR))
                .isInstanceOf(InvalidTrimestralStatusTransitionException.class);
    }

    @Test
    void assertEditableRejectsTerminada() {
        TrimestralPlan plan = TrimestralPlan.create(1L, 10L, "26O", 5L);
        plan.transitionTo(TrimestralPlanStatus.TERMINADA);
        assertThatThrownBy(plan::assertEditable).isInstanceOf(TrimestralPlanNotEditableException.class);
    }

    @Test
    void yearAndTrimesterFromTerm() {
        assertThat(TrimestralPlan.yearFromTerm("26O")).isEqualTo(2026);
        assertThat(TrimestralPlan.trimesterLetter("26I")).isEqualTo('I');
    }

    @Test
    void termNewestFirstOrdersWithinYear() {
        assertThat(TrimestralPlan.termNewestFirst().compare("26O", "26I")).isLessThan(0);
        assertThat(TrimestralPlan.termNewestFirst().compare("26O", "25O")).isLessThan(0);
    }
}

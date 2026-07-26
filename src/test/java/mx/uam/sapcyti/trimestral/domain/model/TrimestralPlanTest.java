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

    @Test
    void scheduleAllowsEmptyDayAndRequiresCompleteStrictRange() {
        TrimestralPlanGroup.validateSlot(new TrimestralPlanGroup.DaySlot(null, null, false));
        TrimestralPlanGroup.validateSlot(new TrimestralPlanGroup.DaySlot("08:00", "10:00", true));

        assertThatThrownBy(() -> TrimestralPlanGroup.validateSlot(
                        new TrimestralPlanGroup.DaySlot("08:00", null, false)))
                .hasMessageContaining("both");
        assertThatThrownBy(() -> TrimestralPlanGroup.validateSlot(
                        new TrimestralPlanGroup.DaySlot("08:00", "08:00", false)))
                .hasMessageContaining("before");
        assertThatThrownBy(() -> TrimestralPlanGroup.validateSlot(
                        new TrimestralPlanGroup.DaySlot("10:00", "08:00", false)))
                .hasMessageContaining("before");
    }

    @Test
    void outdatedIsDerivedFromUniquePersistedReasons() {
        TrimestralPlan plan = TrimestralPlan.create(1L, 10L, "26O", 5L);

        plan.markOutdated(OutdatedReason.SURVEY_REOPENED);
        plan.markOutdated(OutdatedReason.SURVEY_REOPENED);
        plan.markOutdated(OutdatedReason.ANNUAL_PLAN_CHANGED);

        assertThat(plan.isOutdated()).isTrue();
        assertThat(plan.getOutdatedReasons())
                .containsExactly(OutdatedReason.SURVEY_REOPENED, OutdatedReason.ANNUAL_PLAN_CHANGED);

        plan.clearOutdated();
        assertThat(plan.isOutdated()).isFalse();
        assertThat(plan.getOutdatedReasons()).isEmpty();
    }

    @Test
    void labRequiresStartAndEndButEmptyNonLabDayIsValid() {
        assertThatThrownBy(() -> TrimestralPlanGroup.validateSlot(
                        new TrimestralPlanGroup.DaySlot(null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LAB requires");

        TrimestralPlanGroup.validateSlot(new TrimestralPlanGroup.DaySlot(null, null, false));
    }
}

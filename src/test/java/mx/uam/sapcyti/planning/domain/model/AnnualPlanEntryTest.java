package mx.uam.sapcyti.planning.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnnualPlanEntryTest {

    @Test
    @DisplayName("accepts valid group quota and mark values")
    void validValues() {
        AnnualPlanEntry entry = emptyEntry();
        entry.updateValues("2", "15", "*", "*", null, null, Map.of(GraduateProgramMark.PCYTI, "X"));
        assertThat(entry.getGruposI()).isEqualTo("2");
        assertThat(entry.getCupoI()).isEqualTo("15");
        assertThat(entry.getGruposP()).isEqualTo("*");
        assertThat(entry.getPcyti()).isEqualTo("X");
    }

    @Test
    @DisplayName("rejects invalid group quota values")
    void invalidGroupQuota() {
        AnnualPlanEntry entry = emptyEntry();
        assertThatThrownBy(() -> entry.updateValues("abc", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gruposI");
    }

    @Test
    @DisplayName("rejects invalid mark values")
    void invalidMark() {
        AnnualPlanEntry entry = emptyEntry();
        assertThatThrownBy(() -> entry.updateValues(null, null, null, null, null, null,
                        Map.of(GraduateProgramMark.PCYTI, "Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("marks.PCYTI");
    }

    private static AnnualPlanEntry emptyEntry() {
        AnnualPlan plan = AnnualPlan.create(1L, 2027, 1L, java.util.List.of(), java.util.Map.of());
        return plan.getEntries().isEmpty()
                ? AnnualPlanEntry.createEmpty(plan, sampleUea(), (short) 1)
                : plan.getEntries().getFirst();
    }

    private static mx.uam.sapcyti.offering.domain.model.UEA sampleUea() {
        return mx.uam.sapcyti.offering.domain.model.UEA.create(
                1L,
                "2156041",
                "MÉTODOS MATEMÁTICOS",
                mx.uam.sapcyti.offering.domain.model.UeaType.OBLIGATORIA,
                mx.uam.sapcyti.offering.domain.model.UeaModality.MIXTA,
                java.math.BigDecimal.ONE,
                java.math.BigDecimal.ZERO,
                mx.uam.sapcyti.offering.domain.model.FormationType.BASICA,
                9);
    }
}

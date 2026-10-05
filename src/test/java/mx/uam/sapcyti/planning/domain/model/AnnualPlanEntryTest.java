package mx.uam.sapcyti.planning.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnnualPlanEntryTest {

    @Test
    @DisplayName("accepts valid group quota and editable mark values")
    void validValues() {
        AnnualPlanEntry entry = emptyEntry(UeaType.OBLIGATORIA);
        entry.updateValues("2", "15", "*", "*", null, null, Map.of(GraduateProgramMark.P_FIS, "O"));
        assertThat(entry.getGruposI()).isEqualTo("2");
        assertThat(entry.getCupoI()).isEqualTo("15");
        assertThat(entry.getGruposP()).isEqualTo("*");
        assertThat(entry.getPFis()).isEqualTo("O");
    }

    @Test
    @DisplayName("accepts omitted pairs and every positive or wildcard combination")
    void validQuotaPairs() {
        AnnualPlanEntry entry = emptyEntry(UeaType.OBLIGATORIA);

        entry.updateValues(null, null, "2", "*", "*", "15", Map.of());

        assertThat(entry.getGruposI()).isNull();
        assertThat(entry.getCupoI()).isNull();
        assertThat(entry.getGruposP()).isEqualTo("2");
        assertThat(entry.getCupoP()).isEqualTo("*");
        assertThat(entry.getGruposO()).isEqualTo("*");
        assertThat(entry.getCupoO()).isEqualTo("15");
    }

    @Test
    @DisplayName("rejects incomplete and zero-valued quota pairs")
    void invalidQuotaPairs() {
        AnnualPlanEntry entry = emptyEntry(UeaType.OBLIGATORIA);

        assertThatThrownBy(() ->
                        entry.updateValues("1", null, null, null, null, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gruposI and cupoI");
        assertThatThrownBy(() ->
                        entry.updateValues(null, null, "0", "15", null, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gruposP");
    }

    @Test
    @DisplayName("validates every quota pair before changing the entry")
    void quotaUpdateIsAtomic() {
        AnnualPlanEntry entry = emptyEntry(UeaType.OBLIGATORIA);
        entry.updateValues("2", "15", null, null, null, null, Map.of());

        assertThatThrownBy(() ->
                        entry.updateValues("3", "20", "*", null, null, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(entry.getGruposI()).isEqualTo("2");
        assertThat(entry.getCupoI()).isEqualTo("15");
        assertThat(entry.getGruposP()).isNull();
        assertThat(entry.getCupoP()).isNull();
    }

    @Test
    @DisplayName("rejects invalid group quota values")
    void invalidGroupQuota() {
        AnnualPlanEntry entry = emptyEntry(UeaType.OBLIGATORIA);
        assertThatThrownBy(() -> entry.updateValues("abc", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gruposI");
    }

    @Test
    @DisplayName("rejects invalid mark values")
    void invalidMark() {
        AnnualPlanEntry entry = emptyEntry(UeaType.OBLIGATORIA);
        assertThatThrownBy(() -> entry.updateValues(null, null, null, null, null, null,
                        Map.of(GraduateProgramMark.P_FIS, "Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("marks.P_FIS");
    }

    @Test
    @DisplayName("accepts the combined X/O mark on editable programs")
    void acceptsCombinedMark() {
        AnnualPlanEntry entry = emptyEntry(UeaType.OBLIGATORIA);
        entry.updateValues(null, null, null, null, null, null,
                Map.of(GraduateProgramMark.P_FIS, "X/O"));
        assertThat(entry.getPFis()).isEqualTo("X/O");
    }

    @Test
    @DisplayName("derives PCYTI from the UEA type")
    void derivesPcyti() {
        assertThat(emptyEntry(UeaType.OBLIGATORIA).getPcyti()).isEqualTo("X");
        assertThat(emptyEntry(UeaType.OPTATIVA).getPcyti()).isEqualTo("O");
    }

    @Test
    @DisplayName("ignores a PCYTI mark in the payload and keeps the derived value")
    void ignoresPcytiInPayload() {
        AnnualPlanEntry entry = emptyEntry(UeaType.OPTATIVA); // derived -> "O"
        entry.updateValues(null, null, null, null, null, null,
                Map.of(GraduateProgramMark.PCYTI, "X"));
        assertThat(entry.getPcyti()).isEqualTo("O");
    }

    @Test
    @DisplayName("refreshSnapshot re-derives PCYTI from the current type")
    void refreshRederivesPcyti() {
        AnnualPlanEntry entry = emptyEntry(UeaType.OBLIGATORIA); // "X"
        entry.refreshSnapshot(sampleUea(UeaType.OPTATIVA));
        assertThat(entry.getPcyti()).isEqualTo("O");
    }

    private static AnnualPlanEntry emptyEntry(UeaType tipo) {
        AnnualPlan plan = AnnualPlan.create(1L, 2027, 1L, List.of(), Map.of());
        return AnnualPlanEntry.createEmpty(plan, sampleUea(tipo), (short) 1);
    }

    private static UEA sampleUea(UeaType tipo) {
        return UEA.create(
                1L,
                "2156041",
                "MÉTODOS MATEMÁTICOS",
                tipo,
                UeaModality.MIXTA,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                FormationType.BASICA,
                9);
    }
}

package mx.uam.sapcyti.planning.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanEntry;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogSyncServiceTest {

    private final CatalogSyncService catalogSyncService = new CatalogSyncService();

    @Test
    @DisplayName("removes deactivated UEAs and appends new active UEAs with refreshed snapshots")
    void syncCatalog() {
        UEA kept = uea(1L, "2156041", "NOMBRE VIEJO");
        UEA added = uea(3L, "2156100", "NUEVA UEA");
        AnnualPlan plan = AnnualPlan.create(1L, 2027, 1L, List.of(kept, uea(2L, "2156099", "BAJA")), java.util.Map.of());
        AnnualPlanEntry keptEntry = plan.getEntries().stream()
                .filter(entry -> entry.getUeaId().equals(1L))
                .findFirst()
                .orElseThrow();
        keptEntry.updateValues("1", "10", null, null, null, null, Map.of(GraduateProgramMark.PCYTI, "X"));

        catalogSyncService.sync(plan, List.of(
                uea(1L, "2156041", "NOMBRE ACTUALIZADO"),
                added));

        assertThat(plan.getEntries()).hasSize(2);
        AnnualPlanEntry syncedKept = plan.getEntries().stream()
                .filter(entry -> entry.getUeaId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(syncedKept.getNombre()).isEqualTo("NOMBRE ACTUALIZADO");
        assertThat(syncedKept.getGruposI()).isEqualTo("1");
        assertThat(plan.getEntries().stream().anyMatch(entry -> entry.getClave().equals("2156100")))
                .isTrue();
        assertThat(plan.getEntries().stream().anyMatch(entry -> entry.getClave().equals("2156099")))
                .isFalse();
    }

    private static UEA uea(Long id, String clave, String nombre) {
        UEA uea = UEA.create(
                1L,
                clave,
                nombre,
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                FormationType.BASICA,
                9);
        setId(uea, id);
        return uea;
    }

    private static void setId(UEA uea, Long id) {
        try {
            var field = UEA.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(uea, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

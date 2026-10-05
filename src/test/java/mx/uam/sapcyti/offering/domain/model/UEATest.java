package mx.uam.sapcyti.offering.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import mx.uam.sapcyti.offering.domain.exception.ClaveInvalidFormatException;
import mx.uam.sapcyti.offering.domain.exception.UeaAlreadyActiveException;
import mx.uam.sapcyti.offering.domain.exception.UeaAlreadyInactiveException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UEATest {

    @Test
    @DisplayName("create persists explicit credits and defaults active true")
    void createSuccess() {
        UEA uea = UEA.create(
                1L,
                "2156041",
                "MÉTODOS MATEMÁTICOS",
                UeaType.OPTATIVA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.COMPLEMENTARIA,
                9);

        assertThat(uea.getCreditos()).isEqualTo(9);
        assertThat(uea.isActive()).isTrue();
        assertThat(uea.getModalidad()).isEqualTo(UeaModality.MIXTA);
    }

    @Test
    @DisplayName("create rejects non-digit clave")
    void createRejectsInvalidClave() {
        assertThatThrownBy(() -> UEA.create(
                        1L,
                        "ABC-2156041",
                        "Nombre",
                        UeaType.OBLIGATORIA,
                        UeaModality.MIXTA,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        FormationType.BASICA,
                        9))
                .isInstanceOf(ClaveInvalidFormatException.class);
    }

    @Test
    @DisplayName("create rejects non-positive creditos")
    void createRejectsInvalidCreditos() {
        assertThatThrownBy(() -> UEA.create(
                        1L,
                        "2156041",
                        "Nombre",
                        UeaType.OBLIGATORIA,
                        UeaModality.MIXTA,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        FormationType.BASICA,
                        0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update changes editable fields and preserves clave and active")
    void updateSuccess() {
        UEA uea = UEA.create(
                1L,
                "2156041",
                "Nombre original",
                UeaType.OPTATIVA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.COMPLEMENTARIA,
                9);

        uea.update(
                "Nombre actualizado",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                new BigDecimal("4.5"),
                new BigDecimal("0"),
                FormationType.BASICA,
                12);

        assertThat(uea.getClave()).isEqualTo("2156041");
        assertThat(uea.getNombre()).isEqualTo("Nombre actualizado");
        assertThat(uea.getTipo()).isEqualTo(UeaType.OBLIGATORIA);
        assertThat(uea.getHorasTeoria()).isEqualByComparingTo("4.5");
        assertThat(uea.getHorasPractica()).isEqualByComparingTo("0");
        assertThat(uea.getTipoFormacion()).isEqualTo(FormationType.BASICA);
        assertThat(uea.getCreditos()).isEqualTo(12);
        assertThat(uea.isActive()).isTrue();
    }

    @Test
    @DisplayName("update rejects non-positive creditos")
    void updateRejectsInvalidCreditos() {
        UEA uea = UEA.create(
                1L,
                "2156041",
                "Nombre",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                FormationType.BASICA,
                9);

        assertThatThrownBy(() -> uea.update(
                        "Nombre",
                        UeaType.OBLIGATORIA,
                        UeaModality.MIXTA,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        FormationType.BASICA,
                        0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deactivate sets active false and preserves clave")
    void deactivateSuccess() {
        UEA uea = UEA.create(
                1L,
                "2156041",
                "Nombre",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                FormationType.BASICA,
                9);

        uea.deactivate();

        assertThat(uea.isActive()).isFalse();
        assertThat(uea.getClave()).isEqualTo("2156041");
    }

    @Test
    @DisplayName("deactivate rejects already inactive UEA")
    void deactivateRejectsAlreadyInactive() {
        UEA uea = UEA.create(
                1L,
                "2156041",
                "Nombre",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                FormationType.BASICA,
                9);
        uea.deactivate();

        assertThatThrownBy(uea::deactivate)
                .isInstanceOf(UeaAlreadyInactiveException.class);
    }

    @Test
    @DisplayName("restore sets active true and preserves clave and editable fields")
    void restoreSuccess() {
        UEA uea = UEA.create(
                1L,
                "2156041",
                "Nombre",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.BASICA,
                9);
        uea.deactivate();

        uea.restore();

        assertThat(uea.isActive()).isTrue();
        assertThat(uea.getClave()).isEqualTo("2156041");
        assertThat(uea.getNombre()).isEqualTo("Nombre");
        assertThat(uea.getCreditos()).isEqualTo(9);
    }

    @Test
    @DisplayName("restore rejects already active UEA")
    void restoreRejectsAlreadyActive() {
        UEA uea = UEA.create(
                1L,
                "2156041",
                "Nombre",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                FormationType.BASICA,
                9);

        assertThatThrownBy(uea::restore)
                .isInstanceOf(UeaAlreadyActiveException.class);
    }
}

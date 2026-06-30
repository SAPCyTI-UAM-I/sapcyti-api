package mx.uam.sapcyti.offering.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import mx.uam.sapcyti.offering.domain.exception.ClaveInvalidFormatException;
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
}

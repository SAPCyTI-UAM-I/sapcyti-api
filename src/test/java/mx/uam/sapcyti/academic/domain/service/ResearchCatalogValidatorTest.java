package mx.uam.sapcyti.academic.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import mx.uam.sapcyti.academic.domain.port.out.ResearchCatalogPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResearchCatalogValidatorTest {

    private static final String LINE = "Ciencias e Ingeniería de la Computación";
    private static final String AREA = "Inteligencia artificial";

    @Mock private ResearchCatalogPort researchCatalogPort;

    @InjectMocks
    private ResearchCatalogValidator validator;

    @Test
    @DisplayName("HU-44: accepts valid line and area pair")
    void validPair() {
        when(researchCatalogPort.lineExists(LINE)).thenReturn(true);
        when(researchCatalogPort.areaBelongsToLine(LINE, AREA)).thenReturn(true);

        assertThatCode(() -> validator.validate(LINE, AREA)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HU-44: rejects area without line")
    void areaWithoutLine() {
        assertThatThrownBy(() -> validator.validate(null, AREA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ResearchCatalogValidator.LINE_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("HU-44: rejects area not in line")
    void areaMismatch() {
        when(researchCatalogPort.lineExists(LINE)).thenReturn(true);
        when(researchCatalogPort.areaBelongsToLine(LINE, AREA)).thenReturn(false);

        assertThatThrownBy(() -> validator.validate(LINE, AREA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ResearchCatalogValidator.AREA_MISMATCH_MESSAGE);
    }

    @Test
    @DisplayName("HU-44: allows clearing catalog fields")
    void clearFields() {
        assertThatCode(() -> validator.validate(null, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HU-44: rejects unknown line")
    void unknownLine() {
        when(researchCatalogPort.lineExists("Otra línea")).thenReturn(false);

        assertThatThrownBy(() -> validator.validate("Otra línea", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown line of knowledge");
    }
}

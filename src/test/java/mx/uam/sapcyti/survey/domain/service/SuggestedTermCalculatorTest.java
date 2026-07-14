package mx.uam.sapcyti.survey.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuggestedTermCalculatorTest {

    @Test
    @DisplayName("rotates O to I within same year")
    void rotateOtoI() {
        assertThat(SuggestedTermCalculator.suggestNext("26O")).contains("26I");
    }

    @Test
    @DisplayName("rotates I to P within same year")
    void rotateItoP() {
        assertThat(SuggestedTermCalculator.suggestNext("26I")).contains("26P");
    }

    @Test
    @DisplayName("rotates P to next year O")
    void rotatePtoNextO() {
        assertThat(SuggestedTermCalculator.suggestNext("26P")).contains("27O");
    }

    @Test
    @DisplayName("returns empty for invalid term")
    void invalidTerm() {
        assertThat(SuggestedTermCalculator.suggestNext("invalid")).isEmpty();
    }
}

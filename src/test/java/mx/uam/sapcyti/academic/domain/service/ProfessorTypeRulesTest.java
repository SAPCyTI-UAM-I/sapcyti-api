package mx.uam.sapcyti.academic.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.uam.sapcyti.academic.domain.exception.EmployeeNumberImmutableException;
import mx.uam.sapcyti.academic.domain.exception.InvalidTypeChangeException;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfessorTypeRulesTest {

    @Test
    @DisplayName("rejects INTERNO to EXTERNO type change")
    void rejectsInternoToExterno() {
        assertThatThrownBy(() -> ProfessorTypeRules.assertUpdateAllowed(
                        ProfessorType.INTERNO, "30568", ProfessorType.EXTERNO, null))
                .isInstanceOf(InvalidTypeChangeException.class);
    }

    @Test
    @DisplayName("rejects changing assigned employee number")
    void rejectsChangingEmployeeNumber() {
        assertThatThrownBy(() -> ProfessorTypeRules.assertUpdateAllowed(
                        ProfessorType.INTERNO, "30568", ProfessorType.INTERNO, "99999"))
                .isInstanceOf(EmployeeNumberImmutableException.class);
    }

    @Test
    @DisplayName("rejects clearing assigned employee number")
    void rejectsClearingEmployeeNumber() {
        assertThatThrownBy(() -> ProfessorTypeRules.assertUpdateAllowed(
                        ProfessorType.INTERNO, "30568", ProfessorType.INTERNO, null))
                .isInstanceOf(EmployeeNumberImmutableException.class);
    }

    @Test
    @DisplayName("accepts same employee number on update")
    void acceptsSameEmployeeNumber() {
        assertThatCode(() -> ProfessorTypeRules.assertUpdateAllowed(
                        ProfessorType.INTERNO, "30568", ProfessorType.INTERNO, "30568"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("accepts EXTERNO to INTERNO with new employee number")
    void acceptsExternoToInternoWithNemp() {
        assertThatCode(() -> ProfessorTypeRules.assertUpdateAllowed(
                        ProfessorType.EXTERNO, null, ProfessorType.INTERNO, "40123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("accepts EXTERNO update without employee number")
    void acceptsExternoUpdateWithoutNemp() {
        assertThatCode(() -> ProfessorTypeRules.assertUpdateAllowed(
                        ProfessorType.EXTERNO, null, ProfessorType.EXTERNO, null))
                .doesNotThrowAnyException();
    }
}

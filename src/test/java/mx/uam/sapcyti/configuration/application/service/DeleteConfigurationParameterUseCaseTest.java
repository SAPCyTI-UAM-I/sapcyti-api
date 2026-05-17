package mx.uam.sapcyti.configuration.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.uam.sapcyti.configuration.domain.exception.ConfigurationParameterNotFoundException;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;

@ExtendWith(MockitoExtension.class)
class DeleteConfigurationParameterUseCaseTest {

    @Mock
    private GraduateProgramRepositoryPort programRepository;

    @Mock
    private ConfigurationParameterRepositoryPort parameterRepository;

    @InjectMocks
    private DeleteConfigurationParameterUseCase useCase;

    @Test
    @DisplayName("should delete existing parameter")
    void shouldDeleteParameter() {
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(parameterRepository.existsByGraduateProgramIdAndKey(
            1L, "MAX_COURSES_PER_TERM")).thenReturn(true);

        useCase.delete(1L, "MAX_COURSES_PER_TERM");

        verify(parameterRepository).deleteByGraduateProgramIdAndKey(
            1L, "MAX_COURSES_PER_TERM");
    }

    @Test
    @DisplayName("Scenario: Deletion of non-existent parameter")
    void shouldRejectMissingParameter() {
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(parameterRepository.existsByGraduateProgramIdAndKey(1L, "UNKNOWN"))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.delete(1L, "UNKNOWN"))
            .isInstanceOf(ConfigurationParameterNotFoundException.class);

        verify(parameterRepository, never())
            .deleteByGraduateProgramIdAndKey(1L, "UNKNOWN");
    }

    @Test
    @DisplayName("Scenario: Rejection of operation on non-existent program")
    void shouldRejectMissingProgram() {
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(999L, "MAX_COURSES_PER_TERM"))
            .isInstanceOf(GraduateProgramNotFoundException.class);
    }
}

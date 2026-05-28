package mx.uam.sapcyti.configuration.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.uam.sapcyti.configuration.application.command.SetConfigurationParameterCommand;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.ConfigurationParameterResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.ConfigurationParameterMapper;

@ExtendWith(MockitoExtension.class)
class SetConfigurationParameterUseCaseTest {

    @Mock
    private GraduateProgramRepositoryPort programRepository;

    @Mock
    private ConfigurationParameterRepositoryPort parameterRepository;

    @Mock
    private ConfigurationParameterMapper mapper;

    @InjectMocks
    private SetConfigurationParameterUseCase useCase;

    @Test
    @DisplayName("Scenario: Set a new configuration parameter")
    void shouldCreateParameter() {
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");
        SetConfigurationParameterCommand command = new SetConfigurationParameterCommand(
            "MAX_COURSES_PER_TERM", "3", "Maximum UEAs per term");
        ConfigurationParameter saved = new ConfigurationParameter(
            program, command.key(), command.value(), command.description());

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(parameterRepository.findByGraduateProgramIdAndKey(1L, command.key()))
            .thenReturn(Optional.empty());
        when(parameterRepository.save(any(ConfigurationParameter.class)))
            .thenReturn(saved);
        when(mapper.toResponse(saved))
            .thenReturn(new ConfigurationParameterResponse(
                command.key(), command.value(), command.description()));

        useCase.set(1L, command);

        verify(parameterRepository).save(any(ConfigurationParameter.class));
    }

    @Test
    @DisplayName("Scenario: Update the value of an existing parameter")
    void shouldUpdateExistingParameter() {
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");
        ConfigurationParameter existing = new ConfigurationParameter(
            program, "MAX_COURSES_PER_TERM", "2", "old");
        SetConfigurationParameterCommand command = new SetConfigurationParameterCommand(
            "MAX_COURSES_PER_TERM", "3", "updated");

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(parameterRepository.findByGraduateProgramIdAndKey(1L, command.key()))
            .thenReturn(Optional.of(existing));
        when(parameterRepository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing))
            .thenReturn(new ConfigurationParameterResponse(
                command.key(), command.value(), command.description()));

        useCase.set(1L, command);

        org.assertj.core.api.Assertions.assertThat(existing.getValue()).isEqualTo("3");
    }

    @Test
    @DisplayName("Scenario: Rejection of operation on non-existent program")
    void shouldRejectNonExistentProgram() {
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.set(
            999L,
            new SetConfigurationParameterCommand("MAX_COURSES_PER_TERM", "3", null)))
            .isInstanceOf(GraduateProgramNotFoundException.class);
    }
}

package mx.uam.sapcyti.configuration.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.ConfigurationParameterResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.ConfigurationParameterMapper;

@ExtendWith(MockitoExtension.class)
class GetConfigurationParametersUseCaseTest {

    @Mock
    private GraduateProgramRepositoryPort programRepository;

    @Mock
    private ConfigurationParameterRepositoryPort parameterRepository;

    @Mock
    private ConfigurationParameterMapper mapper;

    @InjectMocks
    private GetConfigurationParametersUseCase useCase;

    @Test
    @DisplayName("Scenario: List parameters for a program")
    void shouldListParameters() {
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");
        ConfigurationParameter parameter = new ConfigurationParameter(
            program, "MAX_COURSES_PER_TERM", "3", null);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(parameterRepository.findAllByGraduateProgramId(1L))
            .thenReturn(List.of(parameter));
        when(mapper.toResponseList(List.of(parameter)))
            .thenReturn(List.of(
                new ConfigurationParameterResponse("MAX_COURSES_PER_TERM", "3", null)));

        List<ConfigurationParameterResponse> result = useCase.listByProgram(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Scenario: Get parameter by key")
    void shouldGetByKey() {
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");
        ConfigurationParameter parameter = new ConfigurationParameter(
            program, "MAX_COURSES_PER_TERM", "3", null);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(parameterRepository.findByGraduateProgramIdAndKey(1L, "MAX_COURSES_PER_TERM"))
            .thenReturn(Optional.of(parameter));
        when(mapper.toResponse(parameter))
            .thenReturn(new ConfigurationParameterResponse(
                "MAX_COURSES_PER_TERM", "3", null));

        Optional<ConfigurationParameterResponse> result =
            useCase.getByProgramAndKey(1L, "MAX_COURSES_PER_TERM");

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Scenario: Rejection of operation on non-existent program")
    void shouldRejectMissingProgram() {
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.listByProgram(999L))
            .isInstanceOf(GraduateProgramNotFoundException.class);
    }
}

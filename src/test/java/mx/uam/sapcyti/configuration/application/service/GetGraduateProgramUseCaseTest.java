package mx.uam.sapcyti.configuration.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramListItemResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.GraduateProgramMapper;

@ExtendWith(MockitoExtension.class)
class GetGraduateProgramUseCaseTest {

    @Mock
    private GraduateProgramRepositoryPort programRepository;

    @Mock
    private ConfigurationParameterRepositoryPort parameterRepository;

    @Mock
    private GraduateProgramMapper mapper;

    @InjectMocks
    private GetGraduateProgramUseCase useCase;

    @Test
    @DisplayName("Scenario: Query all graduate programs")
    void shouldListProgramsWithParameterCount() {
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");
        when(programRepository.findAll()).thenReturn(List.of(program));
        when(parameterRepository.countByGraduateProgramId(null)).thenReturn(2L);
        when(mapper.toListItem(program, 2L))
            .thenReturn(new GraduateProgramListItemResponse(
                null, "PCyTI", "CBI", 2L));

        List<GraduateProgramListItemResponse> result = useCase.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).parameterCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Scenario: Query a program with its configuration parameters")
    void shouldGetProgramDetail() {
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(parameterRepository.findAllByGraduateProgramId(1L))
            .thenReturn(List.of());
        when(mapper.toResponse(program, List.of()))
            .thenReturn(new GraduateProgramResponse(1L, "PCyTI", "CBI", List.of()));

        Optional<GraduateProgramResponse> result = useCase.getById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("PCyTI");
    }

    @Test
    @DisplayName("Scenario: Query of non-existent program returns empty")
    void shouldReturnEmptyWhenNotFound() {
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(useCase.getById(999L)).isEmpty();
    }
}

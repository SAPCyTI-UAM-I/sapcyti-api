package mx.uam.sapcyti.configuration.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.uam.sapcyti.configuration.application.command.UpdateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.domain.exception.DuplicateGraduateProgramNameException;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.GraduateProgramMapper;

@ExtendWith(MockitoExtension.class)
class UpdateGraduateProgramUseCaseTest {

    @Mock
    private GraduateProgramRepositoryPort programRepository;

    @Mock
    private ConfigurationParameterRepositoryPort parameterRepository;

    @Mock
    private GraduateProgramMapper mapper;

    @InjectMocks
    private UpdateGraduateProgramUseCase useCase;

    @Test
    @DisplayName("Scenario: Update basic data of a program")
    void shouldUpdateProgram() {
        UpdateGraduateProgramCommand command = new UpdateGraduateProgramCommand(
            1L, "Posgrado en Ciencias y Tecnologías de la Información", "CBI");
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(programRepository.existsByNameAndIdNot(command.name(), 1L))
            .thenReturn(false);
        when(programRepository.save(program)).thenReturn(program);
        when(parameterRepository.findAllByGraduateProgramId(1L)).thenReturn(List.of());
        when(mapper.toResponse(program, List.of()))
            .thenReturn(new GraduateProgramResponse(
                1L, command.name(), command.division(), List.of()));

        useCase.update(command);

        verify(programRepository).save(program);
    }

    @Test
    @DisplayName("Scenario: Rejection of program with duplicate name on update")
    void shouldRejectDuplicateNameOnUpdate() {
        UpdateGraduateProgramCommand command = new UpdateGraduateProgramCommand(
            1L, "Existing Name", "CBI");
        GraduateProgram program = new GraduateProgram("PCyTI", "CBI");

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(programRepository.existsByNameAndIdNot(command.name(), 1L))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.update(command))
            .isInstanceOf(DuplicateGraduateProgramNameException.class);

        verify(programRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario: Query of non-existent program on update")
    void shouldThrowWhenProgramNotFound() {
        UpdateGraduateProgramCommand command = new UpdateGraduateProgramCommand(
            999L, "Name", "CBI");

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.update(command))
            .isInstanceOf(GraduateProgramNotFoundException.class);
    }
}

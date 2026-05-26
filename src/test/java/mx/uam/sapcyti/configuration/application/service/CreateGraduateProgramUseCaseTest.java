package mx.uam.sapcyti.configuration.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.uam.sapcyti.configuration.application.command.CreateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.application.command.InitialParameterCommand;
import mx.uam.sapcyti.configuration.domain.exception.DuplicateGraduateProgramNameException;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.GraduateProgramMapper;

@ExtendWith(MockitoExtension.class)
class CreateGraduateProgramUseCaseTest {

    @Mock
    private GraduateProgramRepositoryPort programRepository;

    @Mock
    private GraduateProgramMapper mapper;

    @InjectMocks
    private CreateGraduateProgramUseCase useCase;

    @Test
    @DisplayName("Scenario: Successful registration of a graduate program")
    void shouldCreateProgram() {
        CreateGraduateProgramCommand command = new CreateGraduateProgramCommand(
            "Ciencias y Tecnologías de la Información", "CBI");
        GraduateProgram saved = new GraduateProgram(command.name(), command.division());
        GraduateProgramResponse response = new GraduateProgramResponse(
            1L, command.name(), command.division(), List.of());

        when(programRepository.existsByName(command.name())).thenReturn(false);
        when(programRepository.save(any(GraduateProgram.class))).thenReturn(saved);
        when(mapper.toResponse(saved, saved.getConfigurationParameters()))
            .thenReturn(response);

        GraduateProgramResponse result = useCase.create(command);

        assertThat(result.id()).isEqualTo(1L);
        verify(programRepository).save(any(GraduateProgram.class));
    }

    @Test
    @DisplayName("Scenario: Rejection of program with duplicate name")
    void shouldRejectDuplicateName() {
        CreateGraduateProgramCommand command = new CreateGraduateProgramCommand(
            "Ciencias y Tecnologías de la Información", "CBI");

        when(programRepository.existsByName(command.name())).thenReturn(true);

        assertThatThrownBy(() -> useCase.create(command))
            .isInstanceOf(DuplicateGraduateProgramNameException.class);

        verify(programRepository, never()).save(any());
    }

    @Test
    @DisplayName("should create program with initial parameters")
    void shouldCreateWithInitialParameters() {
        CreateGraduateProgramCommand command = new CreateGraduateProgramCommand(
            "PCyTI",
            "CBI",
            List.of(new InitialParameterCommand(
                "MAX_COURSES_PER_TERM", "3", "Maximum UEAs per term")));

        GraduateProgram saved = new GraduateProgram(command.name(), command.division());
        when(programRepository.existsByName(command.name())).thenReturn(false);
        when(programRepository.save(any(GraduateProgram.class))).thenReturn(saved);
        when(mapper.toResponse(saved, saved.getConfigurationParameters()))
            .thenReturn(new GraduateProgramResponse(
                1L, "PCyTI", "CBI", List.of()));

        useCase.create(command);

        verify(programRepository).save(any(GraduateProgram.class));
    }
}

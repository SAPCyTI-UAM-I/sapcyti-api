package mx.uam.sapcyti.configuration.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.configuration.application.command.SetConfigurationParameterCommand;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.in.SetConfigurationParameterInputPort;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.ConfigurationParameterResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.ConfigurationParameterMapper;

/**
 * Creates or updates a configuration parameter for a graduate program.
 */
@Service
public class SetConfigurationParameterUseCase implements SetConfigurationParameterInputPort {

    private final GraduateProgramRepositoryPort programRepository;
    private final ConfigurationParameterRepositoryPort parameterRepository;
    private final ConfigurationParameterMapper mapper;

    public SetConfigurationParameterUseCase(
            GraduateProgramRepositoryPort programRepository,
            ConfigurationParameterRepositoryPort parameterRepository,
            ConfigurationParameterMapper mapper) {
        this.programRepository = programRepository;
        this.parameterRepository = parameterRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ConfigurationParameterResponse set(
            Long graduateProgramId,
            SetConfigurationParameterCommand command) {
        GraduateProgram program = programRepository.findById(graduateProgramId)
            .orElseThrow(() -> new GraduateProgramNotFoundException(graduateProgramId));

        ConfigurationParameter parameter = parameterRepository
            .findByGraduateProgramIdAndKey(graduateProgramId, command.key())
            .map(existing -> {
                existing.setValue(command.value());
                existing.setDescription(command.description());
                return existing;
            })
            .orElseGet(() -> new ConfigurationParameter(
                program,
                command.key(),
                command.value(),
                command.description()));

        ConfigurationParameter saved = parameterRepository.save(parameter);
        return mapper.toResponse(saved);
    }
}

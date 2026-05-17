package mx.uam.sapcyti.configuration.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.configuration.application.command.CreateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.application.command.InitialParameterCommand;
import mx.uam.sapcyti.configuration.domain.exception.DuplicateGraduateProgramNameException;
import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.in.CreateGraduateProgramInputPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.GraduateProgramMapper;

/**
 * Creates a graduate program with optional initial configuration parameters.
 */
@Service
public class CreateGraduateProgramUseCase implements CreateGraduateProgramInputPort {

    private final GraduateProgramRepositoryPort programRepository;
    private final GraduateProgramMapper mapper;

    public CreateGraduateProgramUseCase(
            GraduateProgramRepositoryPort programRepository,
            GraduateProgramMapper mapper) {
        this.programRepository = programRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public GraduateProgramResponse create(CreateGraduateProgramCommand command) {
        if (programRepository.existsByName(command.name())) {
            throw new DuplicateGraduateProgramNameException();
        }

        GraduateProgram program = new GraduateProgram(command.name(), command.division());
        List<InitialParameterCommand> initialParameters = command.initialParameters();
        if (initialParameters != null) {
            for (InitialParameterCommand initial : initialParameters) {
                program.addConfigurationParameter(
                    new ConfigurationParameter(
                        program,
                        initial.key(),
                        initial.value(),
                        initial.description()));
            }
        }

        GraduateProgram saved = programRepository.save(program);
        return mapper.toResponse(saved, saved.getConfigurationParameters());
    }
}

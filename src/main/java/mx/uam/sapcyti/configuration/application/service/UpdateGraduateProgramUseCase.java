package mx.uam.sapcyti.configuration.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.configuration.application.command.UpdateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.domain.exception.DuplicateGraduateProgramNameException;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.in.UpdateGraduateProgramInputPort;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.GraduateProgramMapper;

/**
 * Updates basic data of a graduate program without modifying parameters.
 */
@Service
public class UpdateGraduateProgramUseCase implements UpdateGraduateProgramInputPort {

    private final GraduateProgramRepositoryPort programRepository;
    private final ConfigurationParameterRepositoryPort parameterRepository;
    private final GraduateProgramMapper mapper;

    public UpdateGraduateProgramUseCase(
            GraduateProgramRepositoryPort programRepository,
            ConfigurationParameterRepositoryPort parameterRepository,
            GraduateProgramMapper mapper) {
        this.programRepository = programRepository;
        this.parameterRepository = parameterRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public GraduateProgramResponse update(UpdateGraduateProgramCommand command) {
        GraduateProgram program = programRepository.findById(command.id())
            .orElseThrow(() -> new GraduateProgramNotFoundException(command.id()));

        if (programRepository.existsByNameAndIdNot(command.name(), command.id())) {
            throw new DuplicateGraduateProgramNameException();
        }

        program.setName(command.name());
        program.setDivision(command.division());

        GraduateProgram saved = programRepository.save(program);
        var parameters = parameterRepository.findAllByGraduateProgramId(command.id());
        return mapper.toResponse(saved, parameters);
    }
}

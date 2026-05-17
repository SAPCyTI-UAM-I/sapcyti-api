package mx.uam.sapcyti.configuration.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.port.in.GetConfigurationParametersInputPort;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.ConfigurationParameterResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.ConfigurationParameterMapper;

/**
 * Lists and retrieves configuration parameters scoped by program.
 */
@Service
public class GetConfigurationParametersUseCase
        implements GetConfigurationParametersInputPort {

    private final GraduateProgramRepositoryPort programRepository;
    private final ConfigurationParameterRepositoryPort parameterRepository;
    private final ConfigurationParameterMapper mapper;

    public GetConfigurationParametersUseCase(
            GraduateProgramRepositoryPort programRepository,
            ConfigurationParameterRepositoryPort parameterRepository,
            ConfigurationParameterMapper mapper) {
        this.programRepository = programRepository;
        this.parameterRepository = parameterRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfigurationParameterResponse> listByProgram(Long graduateProgramId) {
        assertProgramExists(graduateProgramId);
        return mapper.toResponseList(
            parameterRepository.findAllByGraduateProgramId(graduateProgramId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfigurationParameterResponse> getByProgramAndKey(
            Long graduateProgramId,
            String key) {
        assertProgramExists(graduateProgramId);
        return parameterRepository
            .findByGraduateProgramIdAndKey(graduateProgramId, key)
            .map(mapper::toResponse);
    }

    private void assertProgramExists(Long graduateProgramId) {
        if (programRepository.findById(graduateProgramId).isEmpty()) {
            throw new GraduateProgramNotFoundException(graduateProgramId);
        }
    }
}

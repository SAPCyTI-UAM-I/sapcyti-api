package mx.uam.sapcyti.configuration.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.configuration.domain.exception.ConfigurationParameterNotFoundException;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.port.in.DeleteConfigurationParameterInputPort;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;

/**
 * Deletes a configuration parameter for a graduate program.
 */
@Service
public class DeleteConfigurationParameterUseCase
        implements DeleteConfigurationParameterInputPort {

    private final GraduateProgramRepositoryPort programRepository;
    private final ConfigurationParameterRepositoryPort parameterRepository;

    public DeleteConfigurationParameterUseCase(
            GraduateProgramRepositoryPort programRepository,
            ConfigurationParameterRepositoryPort parameterRepository) {
        this.programRepository = programRepository;
        this.parameterRepository = parameterRepository;
    }

    @Override
    @Transactional
    public void delete(Long graduateProgramId, String key) {
        if (programRepository.findById(graduateProgramId).isEmpty()) {
            throw new GraduateProgramNotFoundException(graduateProgramId);
        }

        if (!parameterRepository.existsByGraduateProgramIdAndKey(
                graduateProgramId, key)) {
            throw new ConfigurationParameterNotFoundException(key);
        }

        parameterRepository.deleteByGraduateProgramIdAndKey(
            graduateProgramId, key);
    }
}

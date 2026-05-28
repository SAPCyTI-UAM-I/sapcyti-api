package mx.uam.sapcyti.configuration.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.configuration.domain.model.ConfigurationParameter;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.domain.port.in.GetGraduateProgramInputPort;
import mx.uam.sapcyti.configuration.domain.port.out.ConfigurationParameterRepositoryPort;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramListItemResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.GraduateProgramMapper;

/**
 * Retrieves graduate programs (list and detail with parameters).
 */
@Service
public class GetGraduateProgramUseCase implements GetGraduateProgramInputPort {

    private final GraduateProgramRepositoryPort programRepository;
    private final ConfigurationParameterRepositoryPort parameterRepository;
    private final GraduateProgramMapper mapper;

    public GetGraduateProgramUseCase(
            GraduateProgramRepositoryPort programRepository,
            ConfigurationParameterRepositoryPort parameterRepository,
            GraduateProgramMapper mapper) {
        this.programRepository = programRepository;
        this.parameterRepository = parameterRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GraduateProgramResponse> getById(Long id) {
        return programRepository.findById(id)
            .map(program -> {
                List<ConfigurationParameter> parameters =
                    parameterRepository.findAllByGraduateProgramId(id);
                return mapper.toResponse(program, parameters);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public List<GraduateProgramListItemResponse> listAll() {
        return programRepository.findAll().stream()
            .map(this::toListItem)
            .toList();
    }

    private GraduateProgramListItemResponse toListItem(GraduateProgram program) {
        long count = parameterRepository.countByGraduateProgramId(program.getId());
        return mapper.toListItem(program, count);
    }
}

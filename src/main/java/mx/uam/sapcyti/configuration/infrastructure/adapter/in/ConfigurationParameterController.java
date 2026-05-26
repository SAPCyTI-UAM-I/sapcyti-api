package mx.uam.sapcyti.configuration.infrastructure.adapter.in;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uam.sapcyti.configuration.domain.exception.ConfigurationParameterNotFoundException;
import mx.uam.sapcyti.configuration.domain.port.in.DeleteConfigurationParameterInputPort;
import mx.uam.sapcyti.configuration.domain.port.in.GetConfigurationParametersInputPort;
import mx.uam.sapcyti.configuration.domain.port.in.SetConfigurationParameterInputPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.ConfigurationParameterResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.SetConfigurationParameterRequest;
import mx.uam.sapcyti.configuration.infrastructure.mapper.ConfigurationParameterMapper;

/**
 * REST API for configuration parameters nested under programs (SPEC-007).
 */
@RestController
@RequestMapping("/api/programs/{programId}/parameters")
public class ConfigurationParameterController {

    private final SetConfigurationParameterInputPort setPort;
    private final GetConfigurationParametersInputPort getPort;
    private final DeleteConfigurationParameterInputPort deletePort;
    private final ConfigurationParameterMapper mapper;

    public ConfigurationParameterController(
            SetConfigurationParameterInputPort setPort,
            GetConfigurationParametersInputPort getPort,
            DeleteConfigurationParameterInputPort deletePort,
            ConfigurationParameterMapper mapper) {
        this.setPort = setPort;
        this.getPort = getPort;
        this.deletePort = deletePort;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ConfigurationParameterResponse set(
            @PathVariable Long programId,
            @Valid @RequestBody SetConfigurationParameterRequest request) {
        return setPort.set(programId, mapper.toSetCommand(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public List<ConfigurationParameterResponse> list(
            @PathVariable Long programId) {
        return getPort.listByProgram(programId);
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ConfigurationParameterResponse getByKey(
            @PathVariable Long programId,
            @PathVariable String key) {
        return getPort.getByProgramAndKey(programId, key)
            .orElseThrow(() -> new ConfigurationParameterNotFoundException(key));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<Void> delete(
            @PathVariable Long programId,
            @PathVariable String key) {
        deletePort.delete(programId, key);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

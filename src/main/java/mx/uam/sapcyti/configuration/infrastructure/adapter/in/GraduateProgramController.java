package mx.uam.sapcyti.configuration.infrastructure.adapter.in;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.port.in.CreateGraduateProgramInputPort;
import mx.uam.sapcyti.configuration.domain.port.in.GetGraduateProgramInputPort;
import mx.uam.sapcyti.configuration.domain.port.in.UpdateGraduateProgramInputPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.CreateGraduateProgramRequest;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramListItemResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.UpdateGraduateProgramRequest;
import mx.uam.sapcyti.configuration.infrastructure.mapper.GraduateProgramMapper;

/**
 * REST API for graduate program management (SPEC-006).
 */
@RestController
@RequestMapping("/api/programs")
public class GraduateProgramController {

    private final CreateGraduateProgramInputPort createPort;
    private final GetGraduateProgramInputPort getPort;
    private final UpdateGraduateProgramInputPort updatePort;
    private final GraduateProgramMapper mapper;

    public GraduateProgramController(
            CreateGraduateProgramInputPort createPort,
            GetGraduateProgramInputPort getPort,
            UpdateGraduateProgramInputPort updatePort,
            GraduateProgramMapper mapper) {
        this.createPort = createPort;
        this.getPort = getPort;
        this.updatePort = updatePort;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<GraduateProgramResponse> create(
            @Valid @RequestBody CreateGraduateProgramRequest request) {
        GraduateProgramResponse body = createPort.create(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(body.id())
            .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public List<GraduateProgramListItemResponse> list() {
        return getPort.listAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public GraduateProgramResponse getById(@PathVariable Long id) {
        return getPort.getById(id)
            .orElseThrow(() -> new GraduateProgramNotFoundException(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public GraduateProgramResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGraduateProgramRequest request) {
        return updatePort.update(mapper.toCommand(id, request));
    }
}

package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.service.GetProfessorUseCase;
import mx.uam.sapcyti.academic.application.service.ListProfessorsUseCase;
import mx.uam.sapcyti.academic.application.service.RegisterProfessorUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.ProfessorResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterProfessorRequest;
import mx.uam.sapcyti.academic.infrastructure.mapper.ProfessorMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/professors")
@RequiredArgsConstructor
public class ProfessorController {

    private final RegisterProfessorUseCase registerProfessorUseCase;
    private final ListProfessorsUseCase listProfessorsUseCase;
    private final GetProfessorUseCase getProfessorUseCase;
    private final ProfessorMapper mapper;

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<ProfessorResponse> register(
            @Valid @RequestBody RegisterProfessorRequest request) {
        RegisterProfessorUseCase.RegisterProfessorResult result =
                registerProfessorUseCase.execute(mapper.toCommand(request));
        ProfessorResponse body = mapper.toResponse(result);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(body.id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public List<ProfessorResponse> list() {
        return listProfessorsUseCase.execute().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ProfessorResponse getById(@PathVariable Long id) {
        return mapper.toResponse(getProfessorUseCase.execute(id));
    }
}

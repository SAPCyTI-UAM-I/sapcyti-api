package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.service.GetProfessorUseCase;
import mx.uam.sapcyti.academic.application.service.ListProfessorsUseCase;
import mx.uam.sapcyti.academic.application.service.RegisterProfessorUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.ProfessorResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterProfessorRequest;
import mx.uam.sapcyti.academic.infrastructure.mapper.ProfessorMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public Page<ProfessorResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        ListProfessorsUseCase.ProfessorListQuery query = new ListProfessorsUseCase.ProfessorListQuery(
                search, active, PageRequest.of(page, size));
        return listProfessorsUseCase.execute(query).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ProfessorResponse getById(@PathVariable Long id) {
        return mapper.toResponse(getProfessorUseCase.execute(id));
    }
}

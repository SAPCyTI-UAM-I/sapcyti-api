package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.service.DeactivateProfessorUseCase;
import mx.uam.sapcyti.academic.application.service.GetProfessorUseCase;
import mx.uam.sapcyti.academic.application.service.ListProfessorsUseCase;
import mx.uam.sapcyti.academic.application.service.RegisterProfessorUseCase;
import mx.uam.sapcyti.academic.application.service.UpdateProfessorUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.ProfessorResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterProfessorRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.UpdateProfessorRequest;
import mx.uam.sapcyti.academic.infrastructure.mapper.ProfessorMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/professors")
@RequiredArgsConstructor
@Tag(name = "Professors", description = "Professor registration and lookup (coordinator-only).")
public class ProfessorController {

    private final RegisterProfessorUseCase registerProfessorUseCase;
    private final ListProfessorsUseCase listProfessorsUseCase;
    private final GetProfessorUseCase getProfessorUseCase;
    private final UpdateProfessorUseCase updateProfessorUseCase;
    private final DeactivateProfessorUseCase deactivateProfessorUseCase;
    private final ProfessorMapper mapper;

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(
            summary = "Register a new professor",
            description = """
                    Registers a professor and creates the associated user account. The server auto-generates a secure \
                    random password (12 characters); there is no separate "generate password" endpoint. The password \
                    is stored BCrypt-hashed and the plaintext is returned exactly once in the `generatedPassword` \
                    field of this response. It is never returned by the list or get-by-id endpoints.""")
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
    @Operation(summary = "List professors", description = "Returns a paginated list of professors. The generated password is never included.")
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
    @Operation(summary = "Get a professor by id", description = "Returns a single professor. The generated password is never included.")
    public ProfessorResponse getById(@PathVariable Long id) {
        return mapper.toResponse(getProfessorUseCase.execute(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update a professor", description = "Updates professor personal and academic fields. Password change is not supported on this endpoint.")
    public ProfessorResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfessorRequest request) {
        return mapper.toResponse(updateProfessorUseCase.execute(mapper.toCommand(id, request)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Deactivate a professor", description = "Logically deactivates the professor by setting the linked user account to inactive.")
    public ProfessorResponse deactivate(@PathVariable Long id) {
        return mapper.toResponse(deactivateProfessorUseCase.execute(id));
    }
}

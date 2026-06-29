package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.service.GetStudentDetailUseCase;
import mx.uam.sapcyti.academic.application.service.ListStudentsUseCase;
import mx.uam.sapcyti.academic.application.service.RegisterStudentUseCase;
import mx.uam.sapcyti.academic.application.service.UpdateStudentUseCase;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterStudentRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentDetailResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.UpdateStudentRequest;
import mx.uam.sapcyti.academic.infrastructure.mapper.StudentMapper;
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
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student registration and lookup (coordinator-only).")
public class StudentController {

    private final RegisterStudentUseCase registerStudentUseCase;
    private final ListStudentsUseCase listStudentsUseCase;
    private final GetStudentDetailUseCase getStudentDetailUseCase;
    private final UpdateStudentUseCase updateStudentUseCase;
    private final StudentMapper mapper;

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(
            summary = "Register a new student",
            description = """
                    Registers a student and creates the associated user account. The server auto-generates a secure \
                    random password (12 characters); there is no separate "generate password" endpoint. The password \
                    is stored BCrypt-hashed and the plaintext is returned exactly once in the `generatedPassword` \
                    field of this response. It is never returned by the list or get-by-id endpoints.""")
    public ResponseEntity<StudentResponse> register(
            @Valid @RequestBody RegisterStudentRequest request) {
        RegisterStudentUseCase.RegisterStudentResult result =
                registerStudentUseCase.execute(mapper.toCommand(request));
        StudentResponse body = mapper.toResponse(result);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(body.id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List students", description = "Returns a paginated list of students. The generated password is never included.")
    public Page<StudentResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProgramType programType,
            @RequestParam(required = false) Boolean active) {
        ListStudentsUseCase.StudentListQuery query = new ListStudentsUseCase.StudentListQuery(
                search, programType, active, PageRequest.of(page, size));
        return listStudentsUseCase.execute(query).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(
            summary = "Get unified student detail",
            description = "Returns student personal data with the embedded academic program (HU-17).")
    public StudentDetailResponse getById(@PathVariable Long id) {
        return mapper.toDetailResponse(getStudentDetailUseCase.execute(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(
            summary = "Update student personal data",
            description = "Updates personal and student-level academic fields. enrollmentId is read-only (HU-18).")
    public StudentResponse update(
            @PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request) {
        return mapper.toResponse(updateStudentUseCase.execute(mapper.toCommand(id, request)));
    }
}

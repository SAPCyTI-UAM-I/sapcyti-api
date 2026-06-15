package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.service.GetStudentUseCase;
import mx.uam.sapcyti.academic.application.service.ListStudentsUseCase;
import mx.uam.sapcyti.academic.application.service.RegisterStudentUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterStudentRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentResponse;
import mx.uam.sapcyti.academic.infrastructure.mapper.StudentMapper;
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
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final RegisterStudentUseCase registerStudentUseCase;
    private final ListStudentsUseCase listStudentsUseCase;
    private final GetStudentUseCase getStudentUseCase;
    private final StudentMapper mapper;

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
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
    public List<StudentResponse> list() {
        return listStudentsUseCase.execute().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public StudentResponse getById(@PathVariable Long id) {
        return mapper.toResponse(getStudentUseCase.execute(id));
    }
}

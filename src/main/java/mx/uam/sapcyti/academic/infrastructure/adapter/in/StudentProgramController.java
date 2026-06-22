package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.service.GetStudentProgramUseCase;
import mx.uam.sapcyti.academic.application.service.ListStudentProgramsUseCase;
import mx.uam.sapcyti.academic.application.service.UpdateStudentProgramUseCase;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentProgramResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.StudentProgramSummaryResponse;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.UpdateStudentProgramRequest;
import mx.uam.sapcyti.academic.infrastructure.mapper.StudentProgramMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/programs")
@RequiredArgsConstructor
@Tag(name = "Student Programs", description = "Student program details and tutor/advisor assignment (coordinator-only).")
public class StudentProgramController {

    private final ListStudentProgramsUseCase listStudentProgramsUseCase;
    private final GetStudentProgramUseCase getStudentProgramUseCase;
    private final UpdateStudentProgramUseCase updateStudentProgramUseCase;
    private final StudentProgramMapper mapper;

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List student programs", description = "Returns all program enrollments for a student in the current tenant.")
    public List<StudentProgramSummaryResponse> list(@PathVariable Long studentId) {
        return listStudentProgramsUseCase.execute(studentId).stream()
                .map(mapper::toSummaryResponse)
                .toList();
    }

    @GetMapping("/{programId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Get student program details", description = "Returns program metadata with resolved tutor and advisor names.")
    public StudentProgramResponse getById(@PathVariable Long studentId, @PathVariable Long programId) {
        return mapper.toResponse(getStudentProgramUseCase.execute(studentId, programId));
    }

    @PutMapping("/{programId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update student program", description = "Updates program metadata and replaces tutor/advisor assignments atomically.")
    public StudentProgramResponse update(
            @PathVariable Long studentId,
            @PathVariable Long programId,
            @Valid @RequestBody UpdateStudentProgramRequest request) {
        GetStudentProgramUseCase.StudentProgramDetail detail = updateStudentProgramUseCase.execute(
                mapper.toCommand(studentId, programId, request));
        return mapper.toResponse(detail);
    }
}

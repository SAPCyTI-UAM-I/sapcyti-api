package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import mx.uam.sapcyti.shared.web.ErrorResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/programs")
@RequiredArgsConstructor
@Tag(
        name = "Student Programs",
        description = """
                View and edit a student's academic program (HU-19, HU-20). Coordinator-only. Requires \
                `Authorization: Bearer <token>` and `X-Graduate-Id` for tenant scope.""")
public class StudentProgramController {

    private final ListStudentProgramsUseCase listStudentProgramsUseCase;
    private final GetStudentProgramUseCase getStudentProgramUseCase;
    private final UpdateStudentProgramUseCase updateStudentProgramUseCase;
    private final StudentProgramMapper mapper;

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(
            summary = "List student programs",
            description = "Returns all program enrollments for a student in the current graduate program tenant.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Programs found for the student in the current tenant.",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = StudentProgramSummaryResponse.class)))),
        @ApiResponse(
                responseCode = "403",
                description = "Caller lacks COORDINATOR role or tenant access.",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student not found in the current tenant.",
                content = @Content(
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(value = """
                                {"error":"NOT_FOUND","message":"Student not found"}
                                """)))
    })
    public List<StudentProgramSummaryResponse> list(
            @Parameter(description = "Student identifier.", required = true, example = "50") @PathVariable Long studentId) {
        return listStudentProgramsUseCase.execute(studentId).stream()
                .map(mapper::toSummaryResponse)
                .toList();
    }

    @GetMapping("/{programId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(
            summary = "Get student program details",
            description = "Returns program metadata with resolved tutor and advisor names for the edit/details screen.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Program details returned.",
                content = @Content(schema = @Schema(implementation = StudentProgramResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Caller lacks COORDINATOR role or tenant access.",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student or program not found in the current tenant.",
                content = @Content(
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(value = """
                                {"error":"NOT_FOUND","message":"Student program not found"}
                                """)))
    })
    public StudentProgramResponse getById(
            @Parameter(description = "Student identifier.", required = true, example = "50") @PathVariable Long studentId,
            @Parameter(description = "Student program identifier.", required = true, example = "100")
                    @PathVariable Long programId) {
        return mapper.toResponse(getStudentProgramUseCase.execute(studentId, programId));
    }

    @PutMapping("/{programId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(
            summary = "Update student program",
            description = """
                    Updates program metadata and replaces tutor/advisor assignments atomically (HU-20). \
                    `programType` and `enrollmentId` are immutable. `advisorIds` replaces the full advisor list. \
                    `tutorId` may be set, changed, or cleared with null.""")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Program updated successfully.",
                content = @Content(schema = @Schema(implementation = StudentProgramResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Validation error (dates, withdrawal reason, duplicate advisorIds).",
                content = @Content(
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(value = """
                                {"error":"VALIDATION_ERROR","message":"Withdrawal reason is required when status is BAJA"}
                                """))),
        @ApiResponse(
                responseCode = "403",
                description = "Caller lacks COORDINATOR role or tenant access.",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student program or referenced professor not found.",
                content = @Content(
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                            @ExampleObject(
                                    name = "Program not found",
                                    value = """
                                            {"error":"NOT_FOUND","message":"Student program not found"}
                                            """),
                            @ExampleObject(
                                    name = "Professor not found",
                                    value = """
                                            {"error":"NOT_FOUND","message":"Professor not found"}
                                            """)
                        }))
    })
    public StudentProgramResponse update(
            @Parameter(description = "Student identifier.", required = true, example = "50") @PathVariable Long studentId,
            @Parameter(description = "Student program identifier.", required = true, example = "100")
                    @PathVariable Long programId,
            @Valid @org.springframework.web.bind.annotation.RequestBody @RequestBody(
                            description = "Updated program fields. Tutor and advisors are replaced atomically.",
                            required = true,
                            content = @Content(
                                    schema = @Schema(implementation = UpdateStudentProgramRequest.class),
                                    examples = {
                                        @ExampleObject(
                                                name = "Assign tutor and advisors",
                                                value = """
                                                        {
                                                          "admissionDate": "2023-09-01",
                                                          "graduationDate": "2026-07-15",
                                                          "researchArea": "Inteligencia Artificial",
                                                          "status": "ACTIVO",
                                                          "withdrawalReason": null,
                                                          "tutorId": 10,
                                                          "advisorIds": [10, 11]
                                                        }
                                                        """),
                                        @ExampleObject(
                                                name = "Record withdrawal",
                                                value = """
                                                        {
                                                          "admissionDate": "2023-09-01",
                                                          "graduationDate": null,
                                                          "researchArea": null,
                                                          "status": "BAJA",
                                                          "withdrawalReason": "Abandono",
                                                          "tutorId": null,
                                                          "advisorIds": []
                                                        }
                                                        """)
                                    }))
                    UpdateStudentProgramRequest request) {
        GetStudentProgramUseCase.StudentProgramDetail detail = updateStudentProgramUseCase.execute(
                mapper.toCommand(studentId, programId, request));
        return mapper.toResponse(detail);
    }
}

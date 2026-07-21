package mx.uam.sapcyti.survey.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.survey.application.service.CloseSurveyUseCase;
import mx.uam.sapcyti.survey.application.service.CreateSurveyUseCase;
import mx.uam.sapcyti.survey.application.service.DeleteSurveyUseCase;
import mx.uam.sapcyti.survey.application.service.GetActiveSurveyForStudentUseCase;
import mx.uam.sapcyti.survey.application.service.GetMyResponseUseCase;
import mx.uam.sapcyti.survey.application.service.GetSurveyResultsSummaryUseCase;
import mx.uam.sapcyti.survey.application.service.GetSurveyUseCase;
import mx.uam.sapcyti.survey.application.service.GetBlankStudentsUseCase;
import mx.uam.sapcyti.survey.application.service.GetUeaDemandUseCase;
import mx.uam.sapcyti.survey.application.service.GetUeaInterestedStudentsUseCase;
import mx.uam.sapcyti.survey.application.service.ListSurveysUseCase;
import mx.uam.sapcyti.survey.application.service.SubmitSurveyResponseUseCase;
import mx.uam.sapcyti.survey.application.service.UpdateSurveyUseCase;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.CreateSurveyRequest;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.EnrollmentSurveyResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.InterestedStudentResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.StudentSurveyFormResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.SubmitResponseRequest;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.SubmittedResponseDto;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.SurveyResultsSummaryResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.UeaDemandRowResponse;
import mx.uam.sapcyti.survey.infrastructure.adapter.in.dto.UpdateSurveyRequest;
import mx.uam.sapcyti.survey.infrastructure.mapper.EnrollmentSurveyMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollment-surveys")
@RequiredArgsConstructor
@Tag(name = "Enrollment Survey", description = "HU-40 configuration, HU-41 student response, HU-42 results.")
public class EnrollmentSurveyController {

    private final CreateSurveyUseCase createSurveyUseCase;
    private final ListSurveysUseCase listSurveysUseCase;
    private final GetSurveyUseCase getSurveyUseCase;
    private final UpdateSurveyUseCase updateSurveyUseCase;
    private final CloseSurveyUseCase closeSurveyUseCase;
    private final DeleteSurveyUseCase deleteSurveyUseCase;
    private final GetActiveSurveyForStudentUseCase getActiveSurveyForStudentUseCase;
    private final SubmitSurveyResponseUseCase submitSurveyResponseUseCase;
    private final GetMyResponseUseCase getMyResponseUseCase;
    private final GetSurveyResultsSummaryUseCase getSurveyResultsSummaryUseCase;
    private final GetUeaDemandUseCase getUeaDemandUseCase;
    private final GetUeaInterestedStudentsUseCase getUeaInterestedStudentsUseCase;
    private final GetBlankStudentsUseCase getBlankStudentsUseCase;
    private final EnrollmentSurveyMapper mapper;

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create enrollment survey", description = "HU-40: creates a PROGRAMADO survey with UEA snapshot.")
    public ResponseEntity<EnrollmentSurveyResponse> create(@Valid @RequestBody CreateSurveyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(createSurveyUseCase.execute(mapper.toCommand(request))));
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List enrollment surveys", description = "HU-40: newest first.")
    public List<EnrollmentSurveyResponse> list() {
        return listSurveysUseCase.execute().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Get enrollment survey", description = "HU-40: survey detail with derived status.")
    public EnrollmentSurveyResponse get(@PathVariable Long id) {
        return mapper.toResponse(getSurveyUseCase.execute(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update or reopen survey", description = "HU-40: edit dates/message or reopen CERRADO survey.")
    public EnrollmentSurveyResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSurveyRequest request) {
        return mapper.toResponse(updateSurveyUseCase.execute(id, mapper.toCommand(request)));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Close survey manually", description = "HU-40: sets closedManually=true.")
    public EnrollmentSurveyResponse close(@PathVariable Long id) {
        return mapper.toResponse(closeSurveyUseCase.execute(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Delete survey", description = "HU-40: only PROGRAMADO without responses.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteSurveyUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Active survey form", description = "HU-41: 404 when no ACTIVO survey.")
    public StudentSurveyFormResponse getActive() {
        return mapper.toResponse(getActiveSurveyForStudentUseCase.execute());
    }

    @PostMapping("/{id}/responses")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit survey response", description = "HU-41: upsert while survey is ACTIVO.")
    public SubmittedResponseDto submit(@PathVariable Long id, @Valid @RequestBody SubmitResponseRequest request) {
        return mapper.toResponse(submitSurveyResponseUseCase.execute(id, mapper.toCommand(request)));
    }

    @GetMapping("/{id}/responses/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get my response", description = "HU-41: read own response in any survey state.")
    public SubmittedResponseDto getMyResponse(@PathVariable Long id) {
        return mapper.toResponse(getMyResponseUseCase.execute(id));
    }

    @GetMapping("/{id}/results/summary")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Survey results summary", description = "HU-42: eligible/responded/pending counts.")
    public SurveyResultsSummaryResponse getResultsSummary(@PathVariable Long id) {
        return mapper.toResponse(getSurveyResultsSummaryUseCase.execute(id));
    }

    @GetMapping("/{id}/results/ueas")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "UEA demand", description = "HU-42: sortable unpaginated demand rows.")
    public List<UeaDemandRowResponse> getUeaDemand(
            @PathVariable Long id,
            @RequestParam(defaultValue = "totalResponses,desc") String sort) {
        return getUeaDemandUseCase.execute(id, sort).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}/results/ueas/{ueaId}/students")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Interested students per UEA", description = "HU-42: students who selected a UEA.")
    public List<InterestedStudentResponse> getInterestedStudents(
            @PathVariable Long id, @PathVariable Long ueaId) {
        return getUeaInterestedStudentsUseCase.execute(id, ueaId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}/results/blank-students")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Blank-enrollment students",
            description = "HU-42: students who answered inscripción en blanco (no UEAs).")
    public List<InterestedStudentResponse> getBlankStudents(@PathVariable Long id) {
        return getBlankStudentsUseCase.execute(id).stream()
                .map(mapper::toResponse)
                .toList();
    }
}

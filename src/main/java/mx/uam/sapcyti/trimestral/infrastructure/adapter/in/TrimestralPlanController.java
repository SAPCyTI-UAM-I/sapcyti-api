package mx.uam.sapcyti.trimestral.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.trimestral.application.service.ChangeTrimestralPlanStatusUseCase;
import mx.uam.sapcyti.trimestral.application.service.ExportTrimestralPlanUseCase;
import mx.uam.sapcyti.trimestral.application.service.ExportTrimestralPlanUseCase.ExportResult;
import mx.uam.sapcyti.trimestral.application.service.GenerateTrimestralPlanUseCase;
import mx.uam.sapcyti.trimestral.application.service.GetTrimestralPlanUseCase;
import mx.uam.sapcyti.trimestral.application.service.GetTrimestralPlanUseCase.PlanDetail;
import mx.uam.sapcyti.trimestral.application.service.ListTrimestralPlansUseCase;
import mx.uam.sapcyti.trimestral.application.service.ListTrimestralPlansUseCase.PlanSummary;
import mx.uam.sapcyti.trimestral.application.service.RegenerateTrimestralPlanUseCase;
import mx.uam.sapcyti.trimestral.application.service.SaveTrimestralPlanGroupsUseCase;
import mx.uam.sapcyti.trimestral.application.service.SaveTrimestralPlanGroupsUseCase.DayScheduleInput;
import mx.uam.sapcyti.trimestral.application.service.SaveTrimestralPlanGroupsUseCase.GroupInput;
import mx.uam.sapcyti.trimestral.application.service.SaveTrimestralPlanGroupsUseCase.StudentInput;
import mx.uam.sapcyti.trimestral.application.service.TrimestralPlanGenerationSupport;
import mx.uam.sapcyti.trimestral.application.service.TrimestralPrerequisiteGuard;
import mx.uam.sapcyti.trimestral.domain.model.ScheduleDay;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto.ChangeTrimestralPlanStatusRequest;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto.CreateTrimestralPlanRequest;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto.SaveTrimestralPlanRequest;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto.SaveTrimestralPlanRequest.DayScheduleRequest;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto.SaveTrimestralPlanRequest.SaveGroupRequest;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto.TrimestralPlanDetailResponse;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto.TrimestralPlanSummaryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trimestral-plans")
@RequiredArgsConstructor
@Tag(name = "Trimestral Planning", description = "HU-58 / HU-59 trimestral plan generation and edit.")
public class TrimestralPlanController {

    private final GenerateTrimestralPlanUseCase generateTrimestralPlanUseCase;
    private final RegenerateTrimestralPlanUseCase regenerateTrimestralPlanUseCase;
    private final ListTrimestralPlansUseCase listTrimestralPlansUseCase;
    private final GetTrimestralPlanUseCase getTrimestralPlanUseCase;
    private final SaveTrimestralPlanGroupsUseCase saveTrimestralPlanGroupsUseCase;
    private final ChangeTrimestralPlanStatusUseCase changeTrimestralPlanStatusUseCase;
    private final ExportTrimestralPlanUseCase exportTrimestralPlanUseCase;
    private final TrimestralPlanGenerationSupport generationSupport;
    private final TrimestralPrerequisiteGuard prerequisiteGuard;

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Generate trimestral plan from a closed survey (HU-58)")
    public ResponseEntity<TrimestralPlanDetailResponse> create(
            @Valid @RequestBody CreateTrimestralPlanRequest request) {
        TrimestralPlan plan = generateTrimestralPlanUseCase.execute(request.surveyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDetail(plan));
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List trimestral plans for tenant (HU-58)")
    public ResponseEntity<List<TrimestralPlanSummaryResponse>> list() {
        List<TrimestralPlanSummaryResponse> plans = listTrimestralPlansUseCase.execute().stream()
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Get trimestral plan detail (HU-58)")
    public ResponseEntity<TrimestralPlanDetailResponse> get(@PathVariable Long id) {
        PlanDetail detail = getTrimestralPlanUseCase.execute(id);
        return ResponseEntity.ok(TrimestralPlanDetailResponse.from(
                detail.plan(), detail.blankStudents(), detail.prerequisites()));
    }

    @PutMapping("/{id}/groups")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Replace all groups of a BORRADOR plan (HU-59)")
    public ResponseEntity<TrimestralPlanDetailResponse> saveGroups(
            @PathVariable Long id, @RequestBody SaveTrimestralPlanRequest request) {
        TrimestralPlan plan = saveTrimestralPlanGroupsUseCase.execute(id, toGroupInputs(request));
        return ResponseEntity.ok(toDetail(plan));
    }

    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Regenerate BORRADOR plan from survey (HU-58)")
    public ResponseEntity<TrimestralPlanDetailResponse> regenerate(@PathVariable Long id) {
        TrimestralPlan plan = regenerateTrimestralPlanUseCase.execute(id);
        return ResponseEntity.ok(toDetail(plan));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Change plan status BORRADOR ⇄ TERMINADA (HU-59)")
    public ResponseEntity<TrimestralPlanDetailResponse> changeStatus(
            @PathVariable Long id, @Valid @RequestBody ChangeTrimestralPlanStatusRequest request) {
        TrimestralPlan plan = changeTrimestralPlanStatusUseCase.execute(id, request.status());
        return ResponseEntity.ok(toDetail(plan));
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Export trimestral plan to official PCyTI Excel (HU-60)")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        ExportResult result = exportTrimestralPlanUseCase.execute(id);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(result.content());
    }

    private TrimestralPlanDetailResponse toDetail(TrimestralPlan plan) {
        return TrimestralPlanDetailResponse.from(
                plan, generationSupport.deriveBlankStudents(plan), prerequisiteGuard.evaluate(plan));
    }

    private TrimestralPlanSummaryResponse toSummary(PlanSummary summary) {
        return TrimestralPlanSummaryResponse.from(summary.plan(), summary.groupCount(), summary.blankCount());
    }

    private static List<GroupInput> toGroupInputs(SaveTrimestralPlanRequest request) {
        if (request == null || request.groups() == null) {
            throw new IllegalArgumentException("groups is required");
        }
        List<GroupInput> inputs = new ArrayList<>();
        for (SaveGroupRequest group : request.groups()) {
            if (group.ueaId() == null) {
                throw new IllegalArgumentException("ueaId is required");
            }
            List<StudentInput> students = group.students() == null
                    ? List.of()
                    : group.students().stream()
                            .map(s -> new StudentInput(s.studentId(), s.obs()))
                            .toList();
            inputs.add(new GroupInput(
                    group.id(),
                    group.ueaId(),
                    group.grupo(),
                    group.cupo(),
                    group.professorIds(),
                    toSchedule(group.schedule()),
                    students));
        }
        return inputs;
    }

    private static List<DayScheduleInput> toSchedule(List<DayScheduleRequest> schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("schedule must contain exactly 5 days LUN..VIE");
        }
        List<DayScheduleInput> result = new ArrayList<>();
        for (DayScheduleRequest day : schedule) {
            if (day == null || day.day() == null || day.day().isBlank()) {
                throw new IllegalArgumentException("schedule day is required");
            }
            ScheduleDay scheduleDay;
            try {
                scheduleDay = ScheduleDay.valueOf(day.day().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("schedule day must be LUN, MAR, MIE, JUE or VIE");
            }
            result.add(new DayScheduleInput(scheduleDay, day.start(), day.end(), day.lab()));
        }
        return result;
    }
}

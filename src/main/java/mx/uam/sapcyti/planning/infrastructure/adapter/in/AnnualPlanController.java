package mx.uam.sapcyti.planning.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.application.service.ChangeStatusUseCase;
import mx.uam.sapcyti.planning.application.service.CheckFormatUseCase;
import mx.uam.sapcyti.planning.application.service.CreateAnnualPlanUseCase;
import mx.uam.sapcyti.planning.application.service.ExportAnnualPlanUseCase;
import mx.uam.sapcyti.planning.application.service.ExportAnnualPlanUseCase.ExportResult;
import mx.uam.sapcyti.planning.application.service.GetAnnualPlanUseCase;
import mx.uam.sapcyti.planning.application.service.ListAnnualPlansUseCase;
import mx.uam.sapcyti.planning.application.service.SaveEntriesUseCase;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.infrastructure.adapter.in.dto.AnnualPlanDetailResponse;
import mx.uam.sapcyti.planning.infrastructure.adapter.in.dto.AnnualPlanSummaryResponse;
import mx.uam.sapcyti.planning.infrastructure.adapter.in.dto.ChangeStatusRequest;
import mx.uam.sapcyti.planning.infrastructure.adapter.in.dto.CreateAnnualPlanRequest;
import mx.uam.sapcyti.planning.infrastructure.adapter.in.dto.FormatCheckReportResponse;
import mx.uam.sapcyti.planning.infrastructure.adapter.in.dto.SaveEntriesRequest;
import mx.uam.sapcyti.planning.infrastructure.mapper.AnnualPlanMapper;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/annual-plans")
@RequiredArgsConstructor
@Tag(name = "Annual Planning", description = "HU-49 to HU-53 annual PCyTI planning.")
public class AnnualPlanController {

    private final CheckFormatUseCase checkFormatUseCase;
    private final CreateAnnualPlanUseCase createAnnualPlanUseCase;
    private final ListAnnualPlansUseCase listAnnualPlansUseCase;
    private final GetAnnualPlanUseCase getAnnualPlanUseCase;
    private final SaveEntriesUseCase saveEntriesUseCase;
    private final ExportAnnualPlanUseCase exportAnnualPlanUseCase;
    private final ChangeStatusUseCase changeStatusUseCase;
    private final AnnualPlanMapper mapper;

    @PostMapping(value = "/check", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Check School Systems Excel against active UEA catalog (HU-49)")
    public ResponseEntity<FormatCheckReportResponse> checkFormat(@RequestPart("file") MultipartFile file)
            throws java.io.IOException {
        CheckFormatUseCase.FormatCheckReport report =
                checkFormatUseCase.execute(file.getOriginalFilename(), file.getBytes());
        return ResponseEntity.ok(FormatCheckReportResponse.from(report));
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create annual plan in BORRADOR (HU-49)")
    public ResponseEntity<AnnualPlanDetailResponse> create(@Valid @RequestBody CreateAnnualPlanRequest request) {
        AnnualPlan plan = createAnnualPlanUseCase.execute(request.year());
        return ResponseEntity.status(HttpStatus.CREATED).body(AnnualPlanDetailResponse.from(plan));
    }

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List annual plans for tenant (HU-50)")
    public ResponseEntity<List<AnnualPlanSummaryResponse>> list() {
        List<AnnualPlanSummaryResponse> plans = listAnnualPlansUseCase.execute().stream()
                .map(AnnualPlanSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{year}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Get annual plan detail with catalog sync in BORRADOR (HU-50)")
    public ResponseEntity<AnnualPlanDetailResponse> get(@PathVariable int year) {
        AnnualPlan plan = getAnnualPlanUseCase.execute(year);
        return ResponseEntity.ok(AnnualPlanDetailResponse.from(plan));
    }

    @PutMapping("/{year}/entries")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Save annual plan entries in BORRADOR (HU-51)")
    public ResponseEntity<AnnualPlanDetailResponse> saveEntries(
            @PathVariable int year, @Valid @RequestBody SaveEntriesRequest request) {
        AnnualPlan plan = saveEntriesUseCase.execute(year, mapper.toEntryUpdates(request));
        return ResponseEntity.ok(AnnualPlanDetailResponse.from(plan));
    }

    @GetMapping("/{year}/export")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Export annual plan to official PCyTI Excel (HU-52)")
    public ResponseEntity<byte[]> export(@PathVariable int year) {
        ExportResult result = exportAnnualPlanUseCase.execute(year);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(result.content());
    }

    @PatchMapping("/{year}/status")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Change annual plan status (HU-53)")
    public ResponseEntity<AnnualPlanSummaryResponse> changeStatus(
            @PathVariable int year, @Valid @RequestBody ChangeStatusRequest request) {
        AnnualPlan plan = changeStatusUseCase.execute(year, request.status());
        return ResponseEntity.ok(AnnualPlanSummaryResponse.from(plan));
    }
}

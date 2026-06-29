package mx.uam.sapcyti.offering.infrastructure.adapter.in;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.application.service.BulkUploadUeasUseCase;
import mx.uam.sapcyti.offering.application.service.ListUeasUseCase;
import mx.uam.sapcyti.offering.application.service.RegisterUeaUseCase;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.RegisterUeaRequest;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.UeaBulkUploadResultResponse;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.UeaCatalogItemResponse;
import mx.uam.sapcyti.offering.infrastructure.mapper.UeaMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ueas")
@RequiredArgsConstructor
@Tag(name = "UEA Catalog", description = "HU-39 catalog list/register and HU-46 bulk upload.")
public class UeaController {

    private final ListUeasUseCase listUeasUseCase;
    private final RegisterUeaUseCase registerUeaUseCase;
    private final BulkUploadUeasUseCase bulkUploadUeasUseCase;
    private final UeaMapper mapper;

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List UEAs", description = "Paginated UEA catalog with optional search and active filter.")
    public Page<UeaCatalogItemResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        return listUeasUseCase
                .execute(search, active, PageRequest.of(page, size))
                .map(mapper::toResponse);
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Register a UEA", description = "Creates a new UEA with active=true.")
    public ResponseEntity<UeaCatalogItemResponse> register(@Valid @RequestBody RegisterUeaRequest request) {
        RegisterUeaUseCase.RegisterUeaResult result =
                registerUeaUseCase.execute(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Bulk upload UEAs", description = "All-or-nothing bulk insert from .xlsx or .csv.")
    public UeaBulkUploadResultResponse bulkUpload(@RequestParam("file") MultipartFile file) throws java.io.IOException {
        BulkUploadUeasUseCase.BulkUploadResult result = bulkUploadUeasUseCase.execute(
                file.getOriginalFilename(), file.getBytes());
        return mapper.toResponse(result);
    }
}

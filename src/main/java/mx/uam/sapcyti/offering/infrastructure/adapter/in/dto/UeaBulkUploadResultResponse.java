package mx.uam.sapcyti.offering.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "UeaBulkUploadResult", description = "Bulk upload outcome (HU-46).")
public record UeaBulkUploadResultResponse(
        int created,
        List<UeaBulkUploadErrorResponse> errors) {
}

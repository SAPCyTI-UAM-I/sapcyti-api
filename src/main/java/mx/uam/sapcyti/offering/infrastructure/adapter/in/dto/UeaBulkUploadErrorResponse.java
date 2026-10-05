package mx.uam.sapcyti.offering.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UeaBulkUploadError", description = "Row-level bulk upload error.")
public record UeaBulkUploadErrorResponse(
        int row,
        String code) {
}

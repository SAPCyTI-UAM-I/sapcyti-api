package mx.uam.sapcyti.planning.infrastructure.adapter.in.dto;

import java.util.List;
import mx.uam.sapcyti.planning.application.service.CheckFormatUseCase.ClaveNombre;
import mx.uam.sapcyti.planning.application.service.CheckFormatUseCase.FormatCheckReport;
import mx.uam.sapcyti.planning.application.service.CheckFormatUseCase.NameMismatch;

public record FormatCheckReportResponse(
        List<ClaveNombreResponse> missingInCatalog,
        List<ClaveNombreResponse> missingInFile,
        List<NameMismatchResponse> nameMismatches,
        List<String> unknownPrograms,
        List<String> missingPrograms) {

    public static FormatCheckReportResponse from(FormatCheckReport report) {
        return new FormatCheckReportResponse(
                report.getMissingInCatalog().stream().map(ClaveNombreResponse::from).toList(),
                report.getMissingInFile().stream().map(ClaveNombreResponse::from).toList(),
                report.getNameMismatches().stream().map(NameMismatchResponse::from).toList(),
                report.getUnknownPrograms(),
                report.getMissingPrograms());
    }

    public record ClaveNombreResponse(String clave, String nombre) {
        static ClaveNombreResponse from(ClaveNombre item) {
            return new ClaveNombreResponse(item.clave(), item.nombre());
        }
    }

    public record NameMismatchResponse(String clave, String nombreCatalogo, String nombreArchivo) {
        static NameMismatchResponse from(NameMismatch item) {
            return new NameMismatchResponse(item.clave(), item.nombreCatalogo(), item.nombreArchivo());
        }
    }
}

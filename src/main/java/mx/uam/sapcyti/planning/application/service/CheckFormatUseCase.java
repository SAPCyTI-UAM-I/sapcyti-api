package mx.uam.sapcyti.planning.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;
import mx.uam.sapcyti.planning.domain.port.out.FormatCheckFileParserPort;
import mx.uam.sapcyti.planning.domain.port.out.FormatCheckFileParserPort.ParsedFormatFile;
import mx.uam.sapcyti.planning.domain.port.out.FormatCheckFileParserPort.ParsedUeaRow;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckFormatUseCase {

    private final FormatCheckFileParserPort fileParser;
    private final UeaRepositoryPort ueaRepository;

    @Transactional(readOnly = true)
    public FormatCheckReport execute(String filename, byte[] content) {
        Long graduateProgramId = requireTenant();
        ParsedFormatFile parsed = fileParser.parse(filename, content);
        List<UEA> activeUeas = ueaRepository.findActiveByGraduateProgramId(graduateProgramId);

        Map<String, UEA> catalogByClave = activeUeas.stream()
                .collect(Collectors.toMap(
                        uea -> uea.getClave().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left));

        Set<String> fileClaves = new HashSet<>();
        List<ClaveNombre> missingInCatalog = new ArrayList<>();
        List<NameMismatch> nameMismatches = new ArrayList<>();

        for (ParsedUeaRow row : parsed.rows()) {
            String normalizedClave = row.clave().toLowerCase(Locale.ROOT);
            fileClaves.add(normalizedClave);
            UEA catalogUea = catalogByClave.get(normalizedClave);
            if (catalogUea == null) {
                missingInCatalog.add(new ClaveNombre(row.clave(), row.nombre()));
            } else if (!catalogUea.getNombre().equalsIgnoreCase(row.nombre().trim())) {
                nameMismatches.add(new NameMismatch(
                        row.clave(), catalogUea.getNombre(), row.nombre()));
            }
        }

        List<ClaveNombre> missingInFile = activeUeas.stream()
                .filter(uea -> !fileClaves.contains(uea.getClave().toLowerCase(Locale.ROOT)))
                .map(uea -> new ClaveNombre(uea.getClave(), uea.getNombre()))
                .toList();

        List<String> systemPrograms = GraduateProgramMark.ALL.stream()
                .map(Enum::name)
                .toList();
        Set<String> filePrograms = parsed.programColumns().stream()
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));

        List<String> unknownPrograms = filePrograms.stream()
                .filter(code -> GraduateProgramMark.fromCode(code).isEmpty())
                .sorted()
                .toList();
        List<String> missingPrograms = systemPrograms.stream()
                .filter(code -> !filePrograms.contains(code))
                .toList();

        return FormatCheckReport.builder()
                .missingInCatalog(missingInCatalog)
                .missingInFile(missingInFile)
                .nameMismatches(nameMismatches)
                .unknownPrograms(unknownPrograms)
                .missingPrograms(missingPrograms)
                .build();
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }

    @Value
    @Builder
    public static class FormatCheckReport {
        List<ClaveNombre> missingInCatalog;
        List<ClaveNombre> missingInFile;
        List<NameMismatch> nameMismatches;
        List<String> unknownPrograms;
        List<String> missingPrograms;
    }

    public record ClaveNombre(String clave, String nombre) {}

    public record NameMismatch(String clave, String nombreCatalogo, String nombreArchivo) {}
}

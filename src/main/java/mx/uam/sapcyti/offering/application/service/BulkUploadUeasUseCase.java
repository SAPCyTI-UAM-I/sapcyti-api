package mx.uam.sapcyti.offering.application.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.port.out.GraduateProgramRepositoryPort;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import mx.uam.sapcyti.offering.domain.port.out.UeaBulkFileParserPort;
import mx.uam.sapcyti.offering.domain.port.out.UeaBulkFileParserPort.ParsedBulkRow;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BulkUploadUeasUseCase {

    private final GraduateProgramRepositoryPort programRepository;
    private final UeaRepositoryPort ueaRepository;
    private final UeaBulkFileParserPort bulkFileParser;

    @Transactional
    public BulkUploadResult execute(String filename, byte[] content) {
        Long graduateProgramId = requireTenant();
        assertProgramExists(graduateProgramId);

        List<ParsedBulkRow> rows = bulkFileParser.parse(filename, content);
        List<BulkUploadError> errors = validateRows(rows, graduateProgramId);
        if (!errors.isEmpty()) {
            return BulkUploadResult.builder().created(0).errors(errors).build();
        }

        List<UEA> ueas = new ArrayList<>();
        for (ParsedBulkRow row : rows) {
            ueas.add(toUea(row, graduateProgramId));
        }
        ueaRepository.saveAll(ueas);
        return BulkUploadResult.builder().created(ueas.size()).errors(List.of()).build();
    }

    private List<BulkUploadError> validateRows(List<ParsedBulkRow> rows, Long graduateProgramId) {
        List<BulkUploadError> errors = new ArrayList<>();
        Set<String> seenClaves = new HashSet<>();

        for (ParsedBulkRow row : rows) {
            validateRow(row, graduateProgramId, seenClaves).ifPresent(errors::add);
        }
        return errors;
    }

    private Optional<BulkUploadError> validateRow(
            ParsedBulkRow row, Long graduateProgramId, Set<String> seenClaves) {
        if (row.parseError()) {
            return Optional.of(error(row.rowNumber(), "INVALID_FORMAT"));
        }

        if (hasInvalidNumericCell(row)) {
            return Optional.of(error(row.rowNumber(), "INVALID_FORMAT"));
        }

        Optional<UeaType> tipo = parseUeaType(row.tipo());
        Optional<FormationType> tipoFormacion = parseFormationType(row.tipoFormacion());
        Optional<UeaModality> modalidad = parseModalidad(row.modalidad());

        if (isBlank(row.clave())
                || isBlank(row.nombre())
                || isBlank(row.tipo())
                || isBlank(row.tipoFormacion())
                || tipo.isEmpty()
                || tipoFormacion.isEmpty()
                || modalidad.isEmpty()) {
            return Optional.of(error(row.rowNumber(), "MISSING_FIELD"));
        }

        Optional<Integer> creditos = parseCreditos(row.creditos());
        if (creditos.isEmpty()) {
            return Optional.of(error(row.rowNumber(), "INVALID_CREDITS"));
        }

        String normalizedClave = row.clave().trim();
        String claveKey = normalizedClave.toLowerCase(Locale.ROOT);
        if (!seenClaves.add(claveKey)
                || ueaRepository.existsByClaveAndGraduateProgramId(normalizedClave, graduateProgramId)) {
            return Optional.of(error(row.rowNumber(), "DUPLICATE_CLAVE"));
        }

        return Optional.empty();
    }

    private static UEA toUea(ParsedBulkRow row, Long graduateProgramId) {
        return UEA.create(
                graduateProgramId,
                row.clave(),
                row.nombre(),
                parseUeaType(row.tipo()).orElseThrow(),
                parseModalidad(row.modalidad()).orElseThrow(),
                parseHoras(row.horasTeoria()),
                parseHoras(row.horasPractica()),
                parseFormationType(row.tipoFormacion()).orElseThrow(),
                parseCreditos(row.creditos()).orElseThrow());
    }

    private static boolean hasInvalidNumericCell(ParsedBulkRow row) {
        return !isBlank(row.horasTeoria()) && parseHoras(row.horasTeoria()) == null
                || !isBlank(row.horasPractica()) && parseHoras(row.horasPractica()) == null;
    }

    private static BigDecimal parseHoras(String value) {
        if (isBlank(value)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim().replace(',', '.'));
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Optional<Integer> parseCreditos(String value) {
        if (isBlank(value)) {
            return Optional.empty();
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim().replace(',', '.'));
            if (parsed.scale() > 0 || parsed.compareTo(BigDecimal.ZERO) <= 0) {
                return Optional.empty();
            }
            return Optional.of(parsed.intValueExact());
        } catch (ArithmeticException | NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static Optional<UeaType> parseUeaType(String value) {
        if (isBlank(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UeaType.valueOf(normalizeEnum(value)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static Optional<FormationType> parseFormationType(String value) {
        if (isBlank(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(FormationType.valueOf(normalizeEnum(value)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static Optional<UeaModality> parseModalidad(String value) {
        if (isBlank(value)) {
            return Optional.empty();
        }
        try {
            UeaModality parsed = UeaModality.valueOf(normalizeEnum(value));
            return parsed == UeaModality.MIXTA ? Optional.of(parsed) : Optional.empty();
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static String normalizeEnum(String value) {
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static BulkUploadError error(int row, String code) {
        return BulkUploadError.builder().row(row).code(code).build();
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }

    private void assertProgramExists(Long graduateProgramId) {
        if (programRepository.findById(graduateProgramId).isEmpty()) {
            throw new GraduateProgramNotFoundException(graduateProgramId);
        }
    }

    @Value
    @Builder
    public static class BulkUploadResult {
        int created;
        List<BulkUploadError> errors;
    }

    @Value
    @Builder
    public static class BulkUploadError {
        int row;
        String code;
    }
}

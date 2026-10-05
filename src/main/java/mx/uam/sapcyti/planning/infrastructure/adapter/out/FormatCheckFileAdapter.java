package mx.uam.sapcyti.planning.infrastructure.adapter.out;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.offering.domain.exception.FileFormatInvalidException;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;
import mx.uam.sapcyti.planning.domain.port.out.FormatCheckFileParserPort;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FormatCheckFileAdapter implements FormatCheckFileParserPort {

    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public ParsedFormatFile parse(String filename, byte[] content) {
        if (filename == null || content == null || content.length == 0) {
            throw new FileFormatInvalidException();
        }
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new FileFormatInvalidException("El archivo debe tener extensión .xlsx");
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            int sectionRow = findSectionRow(sheet);
            if (sectionRow < 0) {
                throw new FileFormatInvalidException(
                        "No se encontró la sección " + PcyotiExcelLayout.SECTION_TITLE + " en la hoja 1");
            }

            int headerRowIndex = findHeaderRow(sheet, sectionRow);
            if (headerRowIndex < 0) {
                throw new FileFormatInvalidException(
                        "Falta la columna requerida: " + PcyotiExcelLayout.CLAVE_HEADER);
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            HeaderMapping header = mapHeader(headerRow);
            List<ParsedUeaRow> rows = readDataRows(sheet, headerRowIndex, header);
            return new ParsedFormatFile(rows, header.programColumns());
        } catch (IOException ex) {
            log.warn("Failed to read annual planning format check file", ex);
            throw new FileFormatInvalidException();
        }
    }

    private int findSectionRow(Sheet sheet) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
                String value = readCell(row.getCell(columnIndex));
                if (value.toUpperCase(Locale.ROOT).contains(PcyotiExcelLayout.SECTION_TITLE.toUpperCase(Locale.ROOT))) {
                    return rowIndex;
                }
            }
        }
        return -1;
    }

    private int findHeaderRow(Sheet sheet, int sectionRow) {
        for (int rowIndex = sectionRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            boolean hasClave = false;
            boolean hasNombre = false;
            for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
                String value = normalizeHeader(readCell(row.getCell(columnIndex)));
                if (PcyotiExcelLayout.CLAVE_HEADER.equalsIgnoreCase(value)) {
                    hasClave = true;
                }
                if (PcyotiExcelLayout.NOMBRE_UEA_HEADER.equalsIgnoreCase(value)) {
                    hasNombre = true;
                }
            }
            if (hasClave && hasNombre) {
                return rowIndex;
            }
        }
        return -1;
    }

    private HeaderMapping mapHeader(Row headerRow) {
        int claveIndex = -1;
        int nombreIndex = -1;
        Set<String> programColumns = new LinkedHashSet<>();

        for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            String value = normalizeHeader(readCell(headerRow.getCell(columnIndex)));
            if (value.isBlank()) {
                continue;
            }
            if (PcyotiExcelLayout.CLAVE_HEADER.equalsIgnoreCase(value)) {
                claveIndex = columnIndex;
            } else if (PcyotiExcelLayout.NOMBRE_UEA_HEADER.equalsIgnoreCase(value)) {
                nombreIndex = columnIndex;
            } else if (GraduateProgramMark.fromCode(value).isPresent()) {
                programColumns.add(value.toUpperCase(Locale.ROOT));
            }
        }

        if (claveIndex < 0) {
            throw new FileFormatInvalidException(
                    "Falta la columna requerida: " + PcyotiExcelLayout.CLAVE_HEADER);
        }
        if (nombreIndex < 0) {
            throw new FileFormatInvalidException(
                    "Falta la columna requerida: " + PcyotiExcelLayout.NOMBRE_UEA_HEADER);
        }

        return new HeaderMapping(claveIndex, nombreIndex, List.copyOf(programColumns));
    }

    private List<ParsedUeaRow> readDataRows(Sheet sheet, int headerRowIndex, HeaderMapping header) {
        List<ParsedUeaRow> rows = new ArrayList<>();
        for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String clave = readCell(row.getCell(header.claveIndex()));
            String nombre = readCell(row.getCell(header.nombreIndex()));
            if (clave.isBlank() && nombre.isBlank()) {
                break;
            }
            if (clave.isBlank()) {
                continue;
            }
            rows.add(new ParsedUeaRow(clave, nombre));
        }
        return rows;
    }

    private String readCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private static String normalizeHeader(String value) {
        return value == null ? "" : value.trim();
    }

    private record HeaderMapping(int claveIndex, int nombreIndex, List<String> programColumns) {}
}

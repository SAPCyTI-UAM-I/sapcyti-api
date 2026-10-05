package mx.uam.sapcyti.offering.infrastructure.adapter.out;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.offering.domain.exception.FileFormatInvalidException;
import mx.uam.sapcyti.offering.domain.port.out.UeaBulkFileParserPort;
import mx.uam.sapcyti.offering.domain.port.out.UeaBulkFileParserPort.ParsedBulkRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UeaBulkFileAdapter implements UeaBulkFileParserPort {

    static final String TEMPLATE_HEADER =
            "Clave,NOMBRE UEA,TIPO,MODALIDAD,H. TEOR.,H. PRAC.,Tipo formacion,Creditos";

    private static final List<String> EXPECTED_HEADERS = List.of(
            "Clave",
            "NOMBRE UEA",
            "TIPO",
            "MODALIDAD",
            "H. TEOR.",
            "H. PRAC.",
            "Tipo formacion",
            "Creditos");

    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public List<ParsedBulkRow> parse(String filename, byte[] content) {
        if (filename == null || content == null || content.length == 0) {
            throw new FileFormatInvalidException();
        }
        String lowerName = filename.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".csv")) {
            return parseCsv(content);
        }
        if (lowerName.endsWith(".xlsx")) {
            return parseXlsx(content);
        }
        throw new FileFormatInvalidException();
    }

    private List<ParsedBulkRow> parseCsv(byte[] content) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            validateHeaderLine(headerLine);

            List<ParsedBulkRow> rows = new ArrayList<>();
            String line;
            int dataRowNumber = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                dataRowNumber++;
                try {
                    List<String> cells = parseCsvLine(line);
                    rows.add(toParsedRow(dataRowNumber, cells));
                } catch (IllegalArgumentException ex) {
                    rows.add(parseErrorRow(dataRowNumber));
                }
            }
            return rows;
        } catch (IOException ex) {
            log.warn("Failed to read CSV bulk upload", ex);
            throw new FileFormatInvalidException();
        }
    }

    private List<ParsedBulkRow> parseXlsx(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new FileFormatInvalidException();
            }
            validateHeaderCells(readRowCells(headerRow, sheet));

            List<ParsedBulkRow> rows = new ArrayList<>();
            int dataRowNumber = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, sheet)) {
                    continue;
                }
                dataRowNumber++;
                if (hasMergedCellsInRow(sheet, rowIndex)) {
                    rows.add(parseErrorRow(dataRowNumber));
                    continue;
                }
                try {
                    rows.add(toParsedRow(dataRowNumber, readRowCells(row, sheet)));
                } catch (IllegalArgumentException ex) {
                    rows.add(parseErrorRow(dataRowNumber));
                }
            }
            return rows;
        } catch (IOException ex) {
            log.warn("Failed to read XLSX bulk upload", ex);
            throw new FileFormatInvalidException();
        }
    }

    private void validateHeaderLine(String headerLine) {
        if (headerLine == null || !headerLine.trim().equals(TEMPLATE_HEADER)) {
            throw new FileFormatInvalidException();
        }
    }

    private void validateHeaderCells(List<String> headers) {
        if (headers.size() != EXPECTED_HEADERS.size()) {
            throw new FileFormatInvalidException();
        }
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            if (!EXPECTED_HEADERS.get(i).equals(headers.get(i))) {
                throw new FileFormatInvalidException();
            }
        }
    }

    private ParsedBulkRow toParsedRow(int dataRowNumber, List<String> cells) {
        if (cells.size() != EXPECTED_HEADERS.size()) {
            throw new IllegalArgumentException("Invalid column count");
        }
        return new ParsedBulkRow(
                dataRowNumber,
                cells.get(0),
                cells.get(1),
                cells.get(2),
                cells.get(3),
                cells.get(4),
                cells.get(5),
                cells.get(6),
                cells.get(7));
    }

    private static ParsedBulkRow parseErrorRow(int dataRowNumber) {
        return new ParsedBulkRow(dataRowNumber, null, null, null, null, null, null, null, null, true);
    }

    private List<String> readRowCells(Row row, Sheet sheet) {
        List<String> cells = new ArrayList<>(EXPECTED_HEADERS.size());
        for (int columnIndex = 0; columnIndex < EXPECTED_HEADERS.size(); columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            cells.add(readCellValue(cell));
        }
        return cells;
    }

    private String readCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private boolean isBlankRow(Row row, Sheet sheet) {
        for (int columnIndex = 0; columnIndex < EXPECTED_HEADERS.size(); columnIndex++) {
            if (!readCellValue(row.getCell(columnIndex)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasMergedCellsInRow(Sheet sheet, int rowIndex) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstRow() <= rowIndex && region.getLastRow() >= rowIndex) {
                return true;
            }
        }
        return false;
    }

    static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString().trim());
        if (cells.size() != EXPECTED_HEADERS.size()) {
            throw new IllegalArgumentException("Invalid column count");
        }
        return cells;
    }
}

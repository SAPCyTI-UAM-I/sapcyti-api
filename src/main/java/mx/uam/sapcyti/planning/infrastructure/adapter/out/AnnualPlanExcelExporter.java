package mx.uam.sapcyti.planning.infrastructure.adapter.out;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanEntry;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnnualPlanExcelExporter {

    public byte[] export(AnnualPlan plan) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Planeacion");

            CellStyle titleStyle = boldItalicCentered(workbook);
            CellStyle boldStyle = bold(workbook);

            List<String> headers = PcyotiExcelLayout.buildHeaders(plan.getYear());
            int lastColumn = headers.size() - 1;
            int rowIndex = 0;

            // Title: "PLANEACION ANUAL {year} POSGRADO", merged across the table width.
            writeMergedTitle(sheet, rowIndex, lastColumn, PcyotiExcelLayout.title(plan.getYear()), titleStyle);
            rowIndex += 2;

            // Legend block: symbol (col A) + description (col B), one per row.
            for (String[] legend : PcyotiExcelLayout.legendRows()) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, legend[0], boldStyle);
                writeCell(row, 1, legend[1], boldStyle);
            }
            rowIndex++;

            // Section title (posgrado), merged.
            writeMergedTitle(sheet, rowIndex, lastColumn, PcyotiExcelLayout.EXPORT_SECTION_TITLE, titleStyle);
            rowIndex += 2;

            // Header row.
            Row headerRow = sheet.createRow(rowIndex++);
            for (int column = 0; column < headers.size(); column++) {
                writeCell(headerRow, column, headers.get(column), boldStyle);
            }

            // One data row per entry (no modalidad column, matching the official format).
            for (AnnualPlanEntry entry : plan.getEntries()) {
                Row row = sheet.createRow(rowIndex++);
                int column = 0;
                row.createCell(column++).setCellValue(entry.getClave());
                row.createCell(column++).setCellValue(entry.getNombre());
                writeGroupQuota(row, column++, entry.getGruposI());
                writeGroupQuota(row, column++, entry.getCupoI());
                writeGroupQuota(row, column++, entry.getGruposP());
                writeGroupQuota(row, column++, entry.getCupoP());
                writeGroupQuota(row, column++, entry.getGruposO());
                writeGroupQuota(row, column++, entry.getCupoO());
                for (GraduateProgramMark mark : GraduateProgramMark.ALL) {
                    String value = entry.getMarks().get(mark);
                    if (value != null) {
                        row.createCell(column).setCellValue(value);
                    }
                    column++;
                }
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            log.error("Failed to export annual plan {}", plan.getYear(), ex);
            throw new IllegalStateException("Failed to export annual plan", ex);
        }
    }

    private void writeMergedTitle(Sheet sheet, int rowIndex, int lastColumn, String text, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        writeCell(row, 0, text, style);
        if (lastColumn > 0) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
        }
    }

    private void writeCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeGroupQuota(Row row, int columnIndex, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Cell cell = row.createCell(columnIndex);
        if ("*".equals(value)) {
            cell.setCellValue("*");
        } else if (value.matches("^[0-9]+$")) {
            cell.setCellValue(Long.parseLong(value));
        } else {
            cell.setCellValue(value);
        }
    }

    private CellStyle boldItalicCentered(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setItalic(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle bold(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }
}

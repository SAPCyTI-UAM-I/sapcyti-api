package mx.uam.sapcyti.planning.infrastructure.adapter.out;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanEntry;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnnualPlanExcelExporter {

    public byte[] export(AnnualPlan plan) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Planeacion");
            int rowIndex = 0;

            sheet.createRow(rowIndex++).createCell(0).setCellValue(PcyotiExcelLayout.SECTION_TITLE);
            for (String legendLine : PcyotiExcelLayout.legendLines()) {
                sheet.createRow(rowIndex++).createCell(0).setCellValue(legendLine);
            }

            List<String> headers = PcyotiExcelLayout.buildHeaders(plan.getYear());
            Row headerRow = sheet.createRow(rowIndex++);
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                headerRow.createCell(columnIndex).setCellValue(headers.get(columnIndex));
            }

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
                row.createCell(column).setCellValue(entry.getModalidad());
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            log.error("Failed to export annual plan {}", plan.getYear(), ex);
            throw new IllegalStateException("Failed to export annual plan", ex);
        }
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
}

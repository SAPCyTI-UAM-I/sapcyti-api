package mx.uam.sapcyti.planning.testutil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import mx.uam.sapcyti.planning.infrastructure.adapter.out.PcyotiExcelLayout;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class PlanningTestXlsx {

    public record UeaRow(String clave, String nombre) {}

    private PlanningTestXlsx() {
    }

    public static byte[] buildSampleFile(int year, List<UeaRow> rows) {
        return buildSampleFile(year, rows, List.of());
    }

    public static byte[] buildSampleFile(int year, List<UeaRow> rows, List<String> extraProgramColumns) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Hoja1");
            int rowIndex = 0;
            sheet.createRow(rowIndex++).createCell(0).setCellValue("Sección " + PcyotiExcelLayout.SECTION_TITLE);

            List<String> headers = new java.util.ArrayList<>(PcyotiExcelLayout.buildHeaders(year));
            for (String extra : extraProgramColumns) {
                headers.add(headers.size() - 1, extra);
            }
            var headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            for (UeaRow row : rows) {
                var dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(row.clave());
                dataRow.createCell(1).setCellValue(row.nombre());
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

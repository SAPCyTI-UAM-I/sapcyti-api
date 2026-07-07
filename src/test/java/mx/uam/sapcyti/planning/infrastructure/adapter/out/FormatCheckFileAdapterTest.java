package mx.uam.sapcyti.planning.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import mx.uam.sapcyti.offering.domain.exception.FileFormatInvalidException;
import mx.uam.sapcyti.planning.testutil.PlanningTestXlsx;
import mx.uam.sapcyti.planning.testutil.PlanningTestXlsx.UeaRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormatCheckFileAdapterTest {

    private final FormatCheckFileAdapter adapter = new FormatCheckFileAdapter();

    @Test
    @DisplayName("parses PCyTI section and program columns")
    void parseValidFile() {
        byte[] content = PlanningTestXlsx.buildSampleFile(
                2027,
                List.of(new UeaRow("2156041", "MÉTODOS MATEMÁTICOS")));

        var parsed = adapter.parse("planeacion.xlsx", content);

        assertThat(parsed.rows()).hasSize(1);
        assertThat(parsed.rows().getFirst().clave()).isEqualTo("2156041");
        assertThat(parsed.programColumns()).contains("PCYTI");
    }

    @Test
    @DisplayName("rejects non-xlsx files")
    void rejectInvalidExtension() {
        assertThatThrownBy(() -> adapter.parse("file.csv", "a,b".getBytes()))
                .isInstanceOf(FileFormatInvalidException.class);
    }

    @Test
    @DisplayName("rejects files without PCyTI section")
    void rejectMissingSection() throws Exception {
        byte[] content;
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                var output = new java.io.ByteArrayOutputStream()) {
            workbook.createSheet().createRow(0).createCell(0).setCellValue("Otra sección");
            workbook.write(output);
            content = output.toByteArray();
        }

        assertThatThrownBy(() -> adapter.parse("file.xlsx", content))
                .isInstanceOf(FileFormatInvalidException.class)
                .hasMessageContaining("PCyTI");
    }
}

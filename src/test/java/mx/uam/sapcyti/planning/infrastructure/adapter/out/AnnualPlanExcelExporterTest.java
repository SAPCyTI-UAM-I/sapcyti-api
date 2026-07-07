package mx.uam.sapcyti.planning.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanEntry;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnnualPlanExcelExporterTest {

    private final AnnualPlanExcelExporter exporter = new AnnualPlanExcelExporter();

    @Test
    @DisplayName("exports legend headers and verbatim cell values")
    void exportLayout() throws Exception {
        UEA uea = UEA.create(
                1L,
                "2156041",
                "MÉTODOS MATEMÁTICOS",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                FormationType.BASICA,
                9);
        AnnualPlan plan = AnnualPlan.create(1L, 2027, 1L, List.of(uea), Map.of());
        AnnualPlanEntry entry = plan.getEntries().getFirst();
        entry.updateValues("2", "15", "*", "*", null, null, Map.of(GraduateProgramMark.PCYTI, "X"));

        byte[] content = exporter.export(plan);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            var sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            // Row 0: dynamic title; rows 2-6: legend block; row 8: section; row 10: headers; row 11: data.
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(0)))
                    .isEqualTo("PLANEACION ANUAL 2027 POSGRADO");
            assertThat(formatter.formatCellValue(sheet.getRow(2).getCell(0))).isEqualTo("Clave");
            assertThat(formatter.formatCellValue(sheet.getRow(2).getCell(1))).isEqualTo("Siete dígitos");
            assertThat(formatter.formatCellValue(sheet.getRow(3).getCell(0))).isEqualTo("X");
            assertThat(formatter.formatCellValue(sheet.getRow(6).getCell(0))).isEqualTo("*");
            assertThat(formatter.formatCellValue(sheet.getRow(8).getCell(0)))
                    .isEqualTo("POSGRADO EN CIENCIAS Y TECNOLOGÍAS DE LA INFORMACIÓN");
            assertThat(formatter.formatCellValue(sheet.getRow(10).getCell(0))).isEqualTo("Clave");
            assertThat(formatter.formatCellValue(sheet.getRow(10).getCell(2)))
                    .isEqualTo("No. de Gpos 27-I");
            assertThat(formatter.formatCellValue(sheet.getRow(10).getCell(14))).isEqualTo("PCyTI");
            assertThat(formatter.formatCellValue(sheet.getRow(11).getCell(0))).isEqualTo("2156041");
            assertThat(formatter.formatCellValue(sheet.getRow(11).getCell(2))).isEqualTo("2");
            assertThat(formatter.formatCellValue(sheet.getRow(11).getCell(3))).isEqualTo("15");
            assertThat(formatter.formatCellValue(sheet.getRow(11).getCell(4))).isEqualTo("*");
        }
    }
}

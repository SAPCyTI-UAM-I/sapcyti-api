package mx.uam.sapcyti.trimestral.infrastructure.adapter.out.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.ScheduleDay;
import mx.uam.sapcyti.trimestral.domain.model.StudentSource;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup.DaySlot;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrimestralPlanExcelExporterTest {

    private final TrimestralPlanExcelExporter exporter = new TrimestralPlanExcelExporter();

    @Test
    @DisplayName("exports Original CyTI headers with TIPO DE UEA in column G")
    void exportLayout() throws Exception {
        TrimestralPlan plan = samplePlan(true, false);
        byte[] content = exporter.export(plan);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            var sheet = workbook.getSheet(TrimestralExcelLayout.SHEET_NAME);
            DataFormatter formatter = new DataFormatter();
            assertThat(sheet).isNotNull();
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(0))).isEqualTo("DIV");
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(6))).isEqualTo("TIPO DE UEA");
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(24))).isEqualTo("OBS");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(0))).isEqualTo("CBI");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(1))).isEqualTo("26O");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(2))).isEqualTo("2156041");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(6))).isEqualTo("OBLIGATORIA");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(11))).isEqualTo("LAB");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(14))).isEmpty();
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(24))).contains("2123999101");
        }
    }

    @Test
    @DisplayName("lists each student on a group row and omits blank-only students")
    void studentsPerGroupRow() throws Exception {
        TrimestralPlan withStudents = samplePlan(true, false);
        byte[] content = exporter.export(withStudents);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            var sheet = workbook.getSheet(TrimestralExcelLayout.SHEET_NAME);
            DataFormatter formatter = new DataFormatter();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(24))).contains("2123999101");
        }

        TrimestralPlan withoutStudents = samplePlan(false, false);
        byte[] emptyStudents = exporter.export(withoutStudents);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(emptyStudents))) {
            assertThat(workbook.getSheet(TrimestralExcelLayout.SHEET_NAME).getPhysicalNumberOfRows())
                    .isEqualTo(2);
        }
    }

    private static TrimestralPlan samplePlan(boolean withStudent, boolean marLab) {
        TrimestralPlan plan = TrimestralPlan.create(1L, 10L, "26O", 5L);
        Map<ScheduleDay, DaySlot> schedule = new EnumMap<>(ScheduleDay.class);
        schedule.put(ScheduleDay.LUN, new DaySlot("08:00", "10:00", true));
        schedule.put(ScheduleDay.MAR, new DaySlot("10:00", "12:00", marLab));
        schedule.put(ScheduleDay.MIE, new DaySlot(null, null, false));
        schedule.put(ScheduleDay.JUE, new DaySlot(null, null, false));
        schedule.put(ScheduleDay.VIE, new DaySlot(null, null, false));

        TrimestralPlanGroup group = TrimestralPlanGroup.createEdited(
                plan,
                1L,
                (short) 1,
                "2156041",
                "MÉTODOS MATEMÁTICOS",
                "OBLIGATORIA",
                "CO43",
                "25",
                null,
                null,
                null,
                null,
                schedule);
        if (withStudent) {
            group.addStudent(GroupStudent.create(
                    group, 50L, "2123999101", "Juan Pérez", StudentSource.SURVEY, "I", (short) 1));
        }
        plan.replaceGroups(List.of(group));
        return plan;
    }
}

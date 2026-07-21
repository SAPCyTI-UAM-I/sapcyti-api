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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrimestralPlanExcelExporterTest {

    private final TrimestralPlanExcelExporter exporter = new TrimestralPlanExcelExporter();

    @Test
    @DisplayName("exports Original CyTI headers with TIPO DE UEA in column G")
    void exportLayout() throws Exception {
        TrimestralPlan plan = samplePlan(List.of(student("2123999101", "Juan Pérez")), null, false);
        byte[] content = exporter.export(plan);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(TrimestralExcelLayout.SHEET_NAME);
            DataFormatter formatter = new DataFormatter();
            assertThat(sheet).isNotNull();
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(0))).isEqualTo("DIV");
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(6))).isEqualTo("TIPO DE UEA");
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(TrimestralExcelLayout.COLUMN_OBS)))
                    .isEqualTo("OBS");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(0))).isEqualTo("CBI");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(1))).isEqualTo("26O");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(2))).isEqualTo("2156041");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(6))).isEqualTo("OBLIGATORIA");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(11))).isEqualTo("LAB");
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(14))).isEmpty();
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(TrimestralExcelLayout.COLUMN_OBS)))
                    .isEmpty();
            assertThat(formatter.formatCellValue(
                            sheet.getRow(1).getCell(TrimestralExcelLayout.COLUMN_STUDENT_NAME)))
                    .isEqualTo("Juan Pérez");
            assertThat(formatter.formatCellValue(
                            sheet.getRow(1).getCell(TrimestralExcelLayout.COLUMN_STUDENT_ENROLLMENT)))
                    .isEqualTo("2123999101");
        }
    }

    @Test
    @DisplayName("lists first student on group row and remaining students on Z/AA continuation rows")
    void studentsOnContinuationRows() throws Exception {
        TrimestralPlan withStudents = samplePlan(
                List.of(
                        student("2123999101", "JHOVANY BADILLO CRUZ"),
                        student("2123999102", "EDGAR SILVA RODRIGUEZ"),
                        student("2123999103", "NATHAEL RAMOS CABRERA")),
                null,
                false);
        byte[] content = exporter.export(withStudents);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(TrimestralExcelLayout.SHEET_NAME);
            DataFormatter formatter = new DataFormatter();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(4);

            Row groupRow = sheet.getRow(1);
            assertThat(formatter.formatCellValue(groupRow.getCell(0))).isEqualTo("CBI");
            assertThat(formatter.formatCellValue(groupRow.getCell(2))).isEqualTo("2156041");
            assertThat(formatter.formatCellValue(groupRow.getCell(TrimestralExcelLayout.COLUMN_OBS)))
                    .isEmpty();
            assertThat(formatter.formatCellValue(groupRow.getCell(TrimestralExcelLayout.COLUMN_STUDENT_NAME)))
                    .isEqualTo("JHOVANY BADILLO CRUZ");
            assertThat(formatter.formatCellValue(
                            groupRow.getCell(TrimestralExcelLayout.COLUMN_STUDENT_ENROLLMENT)))
                    .isEqualTo("2123999101");

            Row continuation = sheet.getRow(2);
            assertThat(continuation.getCell(0)).isNull();
            assertThat(continuation.getCell(2)).isNull();
            assertThat(formatter.formatCellValue(
                            continuation.getCell(TrimestralExcelLayout.COLUMN_STUDENT_NAME)))
                    .isEqualTo("EDGAR SILVA RODRIGUEZ");
            assertThat(formatter.formatCellValue(
                            continuation.getCell(TrimestralExcelLayout.COLUMN_STUDENT_ENROLLMENT)))
                    .isEqualTo("2123999102");

            assertThat(formatter.formatCellValue(
                            sheet.getRow(3).getCell(TrimestralExcelLayout.COLUMN_STUDENT_NAME)))
                    .isEqualTo("NATHAEL RAMOS CABRERA");
        }

        TrimestralPlan withoutStudents = samplePlan(List.of(), null, false);
        byte[] emptyStudents = exporter.export(withoutStudents);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(emptyStudents))) {
            assertThat(workbook.getSheet(TrimestralExcelLayout.SHEET_NAME).getPhysicalNumberOfRows())
                    .isEqualTo(2);
        }
    }

    @Test
    @DisplayName("keeps group OBS in column Y without embedding student listing")
    void groupObsStaysInColumnY() throws Exception {
        TrimestralPlan plan =
                samplePlan(List.of(student("2123999101", "Juan Pérez")), "Nota de grupo", false);

        byte[] content = exporter.export(plan);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(TrimestralExcelLayout.SHEET_NAME);
            DataFormatter formatter = new DataFormatter();
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(TrimestralExcelLayout.COLUMN_OBS)))
                    .isEqualTo("Nota de grupo");
            assertThat(formatter.formatCellValue(
                            sheet.getRow(1).getCell(TrimestralExcelLayout.COLUMN_STUDENT_NAME)))
                    .isEqualTo("Juan Pérez");
        }
    }

    private record StudentFixture(String enrollmentId, String fullName) {}

    private static StudentFixture student(String enrollmentId, String fullName) {
        return new StudentFixture(enrollmentId, fullName);
    }

    private static TrimestralPlan samplePlan(List<StudentFixture> students, String obs, boolean marLab) {
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
                obs,
                schedule);
        short posicion = 1;
        for (StudentFixture student : students) {
            group.addStudent(GroupStudent.create(
                    group,
                    50L + posicion,
                    student.enrollmentId(),
                    student.fullName(),
                    StudentSource.SURVEY,
                    "I",
                    posicion));
            posicion++;
        }
        plan.replaceGroups(List.of(group));
        return plan;
    }
}

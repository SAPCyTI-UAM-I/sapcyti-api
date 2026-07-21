package mx.uam.sapcyti.trimestral.infrastructure.adapter.out.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mx.uam.sapcyti.trimestral.domain.model.GroupProfessor;
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
        TrimestralPlan plan = samplePlan(List.of(student("2123999101", "Juan Pérez")), false);
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

        TrimestralPlan withoutStudents = samplePlan(List.of(), false);
        byte[] emptyStudents = exporter.export(withoutStudents);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(emptyStudents))) {
            assertThat(workbook.getSheet(TrimestralExcelLayout.SHEET_NAME).getPhysicalNumberOfRows())
                    .isEqualTo(2);
        }
    }

    @Test
    @DisplayName("keeps OBS header in column Y but always leaves the cell empty")
    void groupObsColumnAlwaysEmpty() throws Exception {
        TrimestralPlan plan =
                samplePlan(List.of(student("2123999101", "Juan Pérez", "Maestría Física")), false);

        byte[] content = exporter.export(plan);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(TrimestralExcelLayout.SHEET_NAME);
            DataFormatter formatter = new DataFormatter();
            assertThat(formatter.formatCellValue(sheet.getRow(0).getCell(TrimestralExcelLayout.COLUMN_OBS)))
                    .isEqualTo("OBS");
            assertThat(sheet.getRow(1).getCell(TrimestralExcelLayout.COLUMN_OBS)).isNull();
            assertThat(formatter.formatCellValue(
                            sheet.getRow(1).getCell(TrimestralExcelLayout.COLUMN_STUDENT_OBS)))
                    .isEqualTo("Maestría Física");
        }
    }

    @Test
    @DisplayName("writes student obs in AB and one blank row between group blocks")
    void studentObsAndBlankRowBetweenBlocks() throws Exception {
        TrimestralPlan plan = TrimestralPlan.create(1L, 10L, "26O", 5L);
        TrimestralPlanGroup first = sampleGroup(
                plan,
                (short) 1,
                "2156041",
                "MÉTODOS MATEMÁTICOS",
                List.of(
                        student("2123999101", "JHOVANY BADILLO CRUZ", "Maestría Física"),
                        student("2123999102", "EDGAR SILVA RODRIGUEZ", null)),
                false);
        TrimestralPlanGroup second = sampleGroup(
                plan,
                (short) 2,
                "2156047",
                "PROYECTO DE INVESTIGACIÓN II",
                List.of(student("2253800889", "JESUS ALFONSO REYES DE LA VEGA", "PIB")),
                false);
        plan.replaceGroups(List.of(first, second));

        byte[] content = exporter.export(plan);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(TrimestralExcelLayout.SHEET_NAME);
            DataFormatter formatter = new DataFormatter();

            // Block 1: group row (1) + continuation row (2)
            assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(2))).isEqualTo("2156041");
            assertThat(formatter.formatCellValue(
                            sheet.getRow(1).getCell(TrimestralExcelLayout.COLUMN_STUDENT_OBS)))
                    .isEqualTo("Maestría Física");
            assertThat(formatter.formatCellValue(
                            sheet.getRow(2).getCell(TrimestralExcelLayout.COLUMN_STUDENT_NAME)))
                    .isEqualTo("EDGAR SILVA RODRIGUEZ");
            assertThat(sheet.getRow(2).getCell(TrimestralExcelLayout.COLUMN_STUDENT_OBS)).isNull();

            // Row 3 is the blank separator between blocks
            assertThat(sheet.getRow(3)).isNull();

            // Block 2 starts on row 4 with its first student on the group row
            assertThat(formatter.formatCellValue(sheet.getRow(4).getCell(2))).isEqualTo("2156047");
            assertThat(formatter.formatCellValue(
                            sheet.getRow(4).getCell(TrimestralExcelLayout.COLUMN_STUDENT_NAME)))
                    .isEqualTo("JESUS ALFONSO REYES DE LA VEGA");
            assertThat(formatter.formatCellValue(
                            sheet.getRow(4).getCell(TrimestralExcelLayout.COLUMN_STUDENT_OBS)))
                    .isEqualTo("PIB");
            assertThat(sheet.getRow(5)).isNull();
        }
    }

    @Test
    @DisplayName("stacks co-director professors on NEMP/PROF rows alongside the students")
    void professorsStackedOnRows() throws Exception {
        TrimestralPlan plan = TrimestralPlan.create(1L, 10L, "26O", 5L);
        TrimestralPlanGroup group = TrimestralPlanGroup.createEdited(
                plan, 1L, (short) 1, "2156047", "PROYECTO DE INVESTIGACIÓN II", "OBLIGATORIA", "CR43", "1",
                emptySchedule());
        group.replaceProfessors(List.of(
                GroupProfessor.create(group, 41L, "41530", "Leonardo Palacios Luengas", (short) 1),
                GroupProfessor.create(group, 42L, "42178", "Salvador Gonzalez Arellano", (short) 2)));
        group.addStudent(GroupStudent.create(
                group, 300L, "2253800889", "JESUS ALFONSO REYES DE LA VEGA", StudentSource.SURVEY, "IV", null,
                (short) 1));
        plan.replaceGroups(List.of(group));

        byte[] content = exporter.export(plan);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(TrimestralExcelLayout.SHEET_NAME);
            DataFormatter formatter = new DataFormatter();

            // Group row: first professor in H/I, single student in Z.
            Row groupRow = sheet.getRow(1);
            assertThat(formatter.formatCellValue(groupRow.getCell(TrimestralExcelLayout.COLUMN_NEMP)))
                    .isEqualTo("41530");
            assertThat(formatter.formatCellValue(groupRow.getCell(TrimestralExcelLayout.COLUMN_PROF)))
                    .isEqualTo("Leonardo Palacios Luengas");
            assertThat(formatter.formatCellValue(groupRow.getCell(TrimestralExcelLayout.COLUMN_STUDENT_NAME)))
                    .isEqualTo("JESUS ALFONSO REYES DE LA VEGA");

            // Continuation row: second professor in H/I, no student.
            Row continuation = sheet.getRow(2);
            assertThat(formatter.formatCellValue(continuation.getCell(TrimestralExcelLayout.COLUMN_NEMP)))
                    .isEqualTo("42178");
            assertThat(formatter.formatCellValue(continuation.getCell(TrimestralExcelLayout.COLUMN_PROF)))
                    .isEqualTo("Salvador Gonzalez Arellano");
            assertThat(continuation.getCell(TrimestralExcelLayout.COLUMN_STUDENT_NAME)).isNull();
        }
    }

    private static Map<ScheduleDay, DaySlot> emptySchedule() {
        Map<ScheduleDay, DaySlot> map = new EnumMap<>(ScheduleDay.class);
        for (ScheduleDay day : ScheduleDay.values()) {
            map.put(day, new DaySlot(null, null, false));
        }
        return map;
    }

    private record StudentFixture(String enrollmentId, String fullName, String obs) {}

    private static StudentFixture student(String enrollmentId, String fullName) {
        return new StudentFixture(enrollmentId, fullName, null);
    }

    private static StudentFixture student(String enrollmentId, String fullName, String obs) {
        return new StudentFixture(enrollmentId, fullName, obs);
    }

    private static TrimestralPlan samplePlan(List<StudentFixture> students, boolean marLab) {
        TrimestralPlan plan = TrimestralPlan.create(1L, 10L, "26O", 5L);
        plan.replaceGroups(List.of(
                sampleGroup(plan, (short) 1, "2156041", "MÉTODOS MATEMÁTICOS", students, marLab)));
        return plan;
    }

    private static TrimestralPlanGroup sampleGroup(
            TrimestralPlan plan,
            short posicion,
            String clave,
            String nombre,
            List<StudentFixture> students,
            boolean marLab) {
        Map<ScheduleDay, DaySlot> schedule = new EnumMap<>(ScheduleDay.class);
        schedule.put(ScheduleDay.LUN, new DaySlot("08:00", "10:00", true));
        schedule.put(ScheduleDay.MAR, new DaySlot("10:00", "12:00", marLab));
        schedule.put(ScheduleDay.MIE, new DaySlot(null, null, false));
        schedule.put(ScheduleDay.JUE, new DaySlot(null, null, false));
        schedule.put(ScheduleDay.VIE, new DaySlot(null, null, false));

        TrimestralPlanGroup group = TrimestralPlanGroup.createEdited(
                plan, 1L, posicion, clave, nombre, "OBLIGATORIA", "CO43", "25", schedule);
        short studentPos = 1;
        for (StudentFixture student : students) {
            group.addStudent(GroupStudent.create(
                    group,
                    50L + posicion * 100 + studentPos,
                    student.enrollmentId(),
                    student.fullName(),
                    StudentSource.SURVEY,
                    "I",
                    student.obs(),
                    studentPos));
            studentPos++;
        }
        return group;
    }
}

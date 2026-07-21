package mx.uam.sapcyti.trimestral.infrastructure.adapter.out.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import mx.uam.sapcyti.trimestral.domain.model.GroupProfessor;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.ScheduleDay;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup.DaySlot;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrimestralPlanExcelExporter {

    public byte[] export(TrimestralPlan plan) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(TrimestralExcelLayout.SHEET_NAME);

            Row headerRow = sheet.createRow(0);
            List<String> headers = TrimestralExcelLayout.headers();
            for (int column = 0; column < headers.size(); column++) {
                headerRow.createCell(column).setCellValue(headers.get(column));
            }

            int rowIndex = 1;
            for (TrimestralPlanGroup group : plan.getGroups()) {
                if (rowIndex > 1) {
                    rowIndex++; // one blank row between group blocks (reference file has 1..3; we use 1)
                }
                List<GroupStudent> students = group.getStudents();
                List<GroupProfessor> professors = group.getProfessors();
                // Professors (co-directors) and students both stack from the group row down.
                int blockRows = Math.max(1, Math.max(students.size(), professors.size()));
                for (int i = 0; i < blockRows; i++) {
                    GroupProfessor professor = i < professors.size() ? professors.get(i) : null;
                    GroupStudent student = i < students.size() ? students.get(i) : null;
                    if (i == 0) {
                        writeGroupRow(sheet, rowIndex++, plan.getTerm(), group, professor, student);
                    } else {
                        writeContinuationRow(sheet, rowIndex++, professor, student);
                    }
                }
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            log.error("Failed to export trimestral plan {}", plan.getId(), ex);
            throw new IllegalStateException("Failed to export trimestral plan", ex);
        }
    }

    private void writeGroupRow(
            Sheet sheet,
            int rowIndex,
            String term,
            TrimestralPlanGroup group,
            GroupProfessor professor,
            GroupStudent student) {
        Row row = sheet.createRow(rowIndex);
        int column = 0;
        row.createCell(column++).setCellValue(TrimestralExcelLayout.DIVISION);
        row.createCell(column++).setCellValue(term);
        row.createCell(column++).setCellValue(group.getClave());
        row.createCell(column++).setCellValue(group.getNombre());
        writeOptional(row, column++, group.getGrupo());
        writeCupo(row, column++, group.getCupo());
        row.createCell(column++).setCellValue(group.getTipoUea());
        // NEMP (H) / PROF (I): first professor here, the rest stack on continuation rows.
        writeProfessorCells(row, professor);
        column += 2;

        List<DaySlot> schedule = group.scheduleInOrder();
        ScheduleDay[] days = ScheduleDay.values();
        for (int dayIndex = 0; dayIndex < days.length; dayIndex++) {
            DaySlot slot = schedule.get(dayIndex);
            writeOptional(row, column++, slot.start());
            writeOptional(row, column++, slot.end());
            writeLab(row, column++, slot.lab());
        }

        // Col Y (OBS) is kept in the header for positional validation but always stays empty;
        // observations are per student (col AB), matching the official reference files.
        writeStudentCells(row, student);
    }

    /** Continuation rows carry only NEMP/PROF (H/I) and Z/AA/AB; the rest stay empty. */
    private static void writeContinuationRow(
            Sheet sheet, int rowIndex, GroupProfessor professor, GroupStudent student) {
        Row row = sheet.createRow(rowIndex);
        writeProfessorCells(row, professor);
        writeStudentCells(row, student);
    }

    private static void writeProfessorCells(Row row, GroupProfessor professor) {
        if (professor == null) {
            return;
        }
        writeOptional(row, TrimestralExcelLayout.COLUMN_NEMP, professor.getEmployeeNumber());
        writeOptional(row, TrimestralExcelLayout.COLUMN_PROF, professor.getProfessorName());
    }

    private static void writeStudentCells(Row row, GroupStudent student) {
        if (student == null) {
            return;
        }
        writeOptional(row, TrimestralExcelLayout.COLUMN_STUDENT_NAME, student.getFullName());
        writeEnrollment(row, TrimestralExcelLayout.COLUMN_STUDENT_ENROLLMENT, student.getEnrollmentId());
        writeOptional(row, TrimestralExcelLayout.COLUMN_STUDENT_OBS, student.getObs());
    }

    private static void writeLab(Row row, int column, boolean lab) {
        if (lab) {
            row.createCell(column).setCellValue("LAB");
        }
    }

    private static void writeOptional(Row row, int column, String value) {
        if (value != null && !value.isBlank()) {
            row.createCell(column).setCellValue(value);
        }
    }

    private static void writeEnrollment(Row row, int column, String enrollmentId) {
        if (enrollmentId == null || enrollmentId.isBlank()) {
            return;
        }
        Cell cell = row.createCell(column);
        if (enrollmentId.matches("^[0-9]+$")) {
            cell.setCellValue(Long.parseLong(enrollmentId));
        } else {
            cell.setCellValue(enrollmentId);
        }
    }

    private static void writeCupo(Row row, int column, String cupo) {
        if (cupo == null || cupo.isBlank()) {
            return;
        }
        Cell cell = row.createCell(column);
        if ("*".equals(cupo)) {
            cell.setCellValue("*");
        } else if (cupo.matches("^[0-9]+$")) {
            cell.setCellValue(Long.parseLong(cupo));
        } else {
            cell.setCellValue(cupo);
        }
    }

}

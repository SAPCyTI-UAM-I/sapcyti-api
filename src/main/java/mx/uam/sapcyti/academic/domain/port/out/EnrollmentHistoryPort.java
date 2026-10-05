package mx.uam.sapcyti.academic.domain.port.out;

import java.util.List;

public interface EnrollmentHistoryPort {

    List<EnrollmentHistoryEntry> findByStudent(Long studentId, Long graduateProgramId);

    record EnrollmentHistoryEntry(
            String term,
            String academicTermSelected,
            String mode,
            HistoryPlanStatus planStatus,
            String note,
            List<EnrollmentHistoryUea> ueas) {}

    record EnrollmentHistoryUea(
            HistoryUeaStatus status,
            String clave,
            String nombre,
            String grupo,
            List<HistoryProfessor> professors,
            List<DaySchedule> schedule) {}

    record HistoryProfessor(Long professorId, String employeeNumber, String professorName) {}

    record DaySchedule(String day, String start, String end, boolean lab) {}

    enum HistoryPlanStatus {
        PENDING,
        TERMINADA
    }

    enum HistoryUeaStatus {
        PENDING,
        ASSIGNED,
        REMOVED_FROM_FINAL_PLAN
    }
}

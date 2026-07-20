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
            String clave,
            String nombre,
            String grupo,
            String professorName,
            List<DaySchedule> schedule) {}

    record DaySchedule(String day, String start, String end, boolean lab) {}

    enum HistoryPlanStatus {
        PENDING,
        TERMINADA
    }
}

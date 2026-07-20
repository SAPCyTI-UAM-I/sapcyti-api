package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import java.util.List;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.DaySchedule;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.EnrollmentHistoryEntry;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.EnrollmentHistoryUea;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.HistoryPlanStatus;

public record EnrollmentHistoryEntryResponse(
        String term,
        String academicTermSelected,
        String mode,
        HistoryPlanStatus planStatus,
        String note,
        List<EnrollmentHistoryUeaResponse> ueas) {

    public static EnrollmentHistoryEntryResponse from(EnrollmentHistoryEntry entry) {
        List<EnrollmentHistoryUeaResponse> ueas = entry.ueas().stream()
                .map(EnrollmentHistoryUeaResponse::from)
                .toList();
        return new EnrollmentHistoryEntryResponse(
                entry.term(),
                entry.academicTermSelected(),
                entry.mode(),
                entry.planStatus(),
                entry.note(),
                ueas);
    }

    public record EnrollmentHistoryUeaResponse(
            String clave,
            String nombre,
            String grupo,
            String professorName,
            List<DayScheduleResponse> schedule) {

        static EnrollmentHistoryUeaResponse from(EnrollmentHistoryUea uea) {
            List<DayScheduleResponse> schedule = uea.schedule() == null
                    ? null
                    : uea.schedule().stream().map(DayScheduleResponse::from).toList();
            return new EnrollmentHistoryUeaResponse(
                    uea.clave(), uea.nombre(), uea.grupo(), uea.professorName(), schedule);
        }
    }

    public record DayScheduleResponse(String day, String start, String end, boolean lab) {
        static DayScheduleResponse from(DaySchedule schedule) {
            return new DayScheduleResponse(schedule.day(), schedule.start(), schedule.end(), schedule.lab());
        }
    }
}

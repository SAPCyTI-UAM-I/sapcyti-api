package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import java.util.List;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.DaySchedule;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.EnrollmentHistoryEntry;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.EnrollmentHistoryUea;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.HistoryProfessor;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.HistoryPlanStatus;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.HistoryUeaStatus;

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
            HistoryUeaStatus status,
            String clave,
            String nombre,
            String grupo,
            List<HistoryProfessorResponse> professors,
            List<DayScheduleResponse> schedule) {

        static EnrollmentHistoryUeaResponse from(EnrollmentHistoryUea uea) {
            List<HistoryProfessorResponse> professors = uea.professors().stream()
                    .map(HistoryProfessorResponse::from)
                    .toList();
            List<DayScheduleResponse> schedule = uea.schedule() == null
                    ? null
                    : uea.schedule().stream().map(DayScheduleResponse::from).toList();
            return new EnrollmentHistoryUeaResponse(
                    uea.status(), uea.clave(), uea.nombre(), uea.grupo(), professors, schedule);
        }
    }

    public record HistoryProfessorResponse(Long professorId, String employeeNumber, String professorName) {
        static HistoryProfessorResponse from(HistoryProfessor professor) {
            return new HistoryProfessorResponse(
                    professor.professorId(), professor.employeeNumber(), professor.professorName());
        }
    }

    public record DayScheduleResponse(String day, String start, String end, boolean lab) {
        static DayScheduleResponse from(DaySchedule schedule) {
            return new DayScheduleResponse(schedule.day(), schedule.start(), schedule.end(), schedule.lab());
        }
    }
}

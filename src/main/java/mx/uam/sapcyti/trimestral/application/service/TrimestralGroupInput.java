package mx.uam.sapcyti.trimestral.application.service;

import java.util.List;
import mx.uam.sapcyti.trimestral.domain.model.ScheduleDay;

public record TrimestralGroupInput(
        Long id,
        Long ueaId,
        String grupo,
        String cupo,
        List<Long> professorIds,
        List<DaySchedule> schedule,
        List<Student> students) {

    public TrimestralGroupInput {
        professorIds = professorIds == null ? List.of() : List.copyOf(professorIds);
        schedule = schedule == null ? null : List.copyOf(schedule);
        students = students == null ? List.of() : List.copyOf(students);
    }

    public record Student(Long studentId, String obs) {
    }

    public record DaySchedule(ScheduleDay day, String start, String end, boolean lab) {
    }
}

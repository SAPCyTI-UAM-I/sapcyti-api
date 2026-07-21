package mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto;

import java.util.List;

/**
 * Nested format validation is done in the use case (after BORRADOR check), not via Bean Validation.
 */
public record SaveTrimestralPlanRequest(List<SaveGroupRequest> groups) {

    public record SaveGroupRequest(
            Long id,
            Long ueaId,
            String grupo,
            String cupo,
            Long professorId,
            List<DayScheduleRequest> schedule,
            List<SaveGroupStudentRequest> students) {}

    public record SaveGroupStudentRequest(Long studentId, String obs) {}

    public record DayScheduleRequest(String day, String start, String end, boolean lab) {}
}

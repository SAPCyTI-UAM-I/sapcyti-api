package mx.uam.sapcyti.trimestral.infrastructure.adapter.in.dto;

import java.util.ArrayList;
import java.util.List;
import mx.uam.sapcyti.trimestral.application.service.TrimestralPlanGenerationSupport.BlankStudentView;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.PlanWarning;
import mx.uam.sapcyti.trimestral.domain.model.ScheduleDay;
import mx.uam.sapcyti.trimestral.domain.model.StudentSource;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup.DaySlot;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanStatus;
import mx.uam.sapcyti.trimestral.domain.model.WarningCode;

public record TrimestralPlanDetailResponse(
        Long id,
        String term,
        TrimestralPlanStatus status,
        Long surveyId,
        boolean outdated,
        List<TrimestralGroupResponse> groups,
        List<BlankStudentResponse> blankStudents,
        List<PlanWarningResponse> warnings) {

    public static TrimestralPlanDetailResponse from(TrimestralPlan plan, List<BlankStudentView> blanks) {
        List<TrimestralGroupResponse> groups = plan.getGroups().stream()
                .map(TrimestralGroupResponse::from)
                .toList();
        List<BlankStudentResponse> blankStudents = blanks.stream()
                .map(b -> new BlankStudentResponse(b.studentId(), b.enrollmentId(), b.fullName(), b.academicTerm()))
                .toList();
        List<PlanWarningResponse> warnings = plan.getWarnings().stream()
                .map(PlanWarningResponse::from)
                .toList();
        return new TrimestralPlanDetailResponse(
                plan.getId(),
                plan.getTerm(),
                plan.getStatus(),
                plan.getSurveyId(),
                plan.isOutdated(),
                groups,
                blankStudents,
                warnings);
    }

    public record TrimestralGroupResponse(
            Long id,
            Long ueaId,
            String clave,
            String nombre,
            String tipoUea,
            String grupo,
            String cupo,
            String employeeNumber,
            Long professorId,
            String professorName,
            List<DayScheduleResponse> schedule,
            List<GroupStudentResponse> students) {

        static TrimestralGroupResponse from(TrimestralPlanGroup group) {
            List<DayScheduleResponse> schedule = new ArrayList<>();
            ScheduleDay[] days = ScheduleDay.values();
            List<DaySlot> slots = group.scheduleInOrder();
            for (int i = 0; i < days.length; i++) {
                DaySlot slot = slots.get(i);
                schedule.add(new DayScheduleResponse(days[i].name(), slot.start(), slot.end(), slot.lab()));
            }
            List<GroupStudentResponse> students = group.getStudents().stream()
                    .map(GroupStudentResponse::from)
                    .toList();
            return new TrimestralGroupResponse(
                    group.getId(),
                    group.getUeaId(),
                    group.getClave(),
                    group.getNombre(),
                    group.getTipoUea(),
                    group.getGrupo(),
                    group.getCupo(),
                    group.getEmployeeNumber(),
                    group.getProfessorId(),
                    group.getProfessorName(),
                    schedule,
                    students);
        }
    }

    public record DayScheduleResponse(String day, String start, String end, boolean lab) {}

    public record GroupStudentResponse(
            Long studentId,
            String enrollmentId,
            String fullName,
            StudentSource source,
            String academicTerm,
            String obs) {

        static GroupStudentResponse from(GroupStudent student) {
            return new GroupStudentResponse(
                    student.getStudentId(),
                    student.getEnrollmentId(),
                    student.getFullName(),
                    student.getSource(),
                    student.getAcademicTerm(),
                    student.getObs());
        }
    }

    public record BlankStudentResponse(
            Long studentId, String enrollmentId, String fullName, String academicTerm) {}

    public record PlanWarningResponse(
            WarningCode code,
            String clave,
            String enrollmentId,
            String employeeNumber,
            Long groupId) {

        static PlanWarningResponse from(PlanWarning warning) {
            return new PlanWarningResponse(
                    warning.getCode(),
                    warning.getClave(),
                    warning.getEnrollmentId(),
                    warning.getEmployeeNumber(),
                    warning.getGroupId());
        }
    }
}

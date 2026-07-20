package mx.uam.sapcyti.trimestral.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.PlanWarning;
import mx.uam.sapcyti.trimestral.domain.model.StudentSource;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.WarningCode;
import mx.uam.sapcyti.trimestral.domain.service.WarningEngine.WarningContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WarningEngineTest {

    private WarningEngine engine;
    private TrimestralPlan plan;

    @BeforeEach
    void setUp() {
        engine = new WarningEngine();
        plan = TrimestralPlan.create(1L, 10L, "26O", 5L);
    }

    @Test
    void noResponsesWarning() {
        List<PlanWarning> warnings = engine.evaluate(plan, new WarningContext(true, Set.of(), Set.of(), Set.of(), Set.of()));
        assertThat(warnings).extracting(PlanWarning::getCode).containsExactly(WarningCode.NO_RESPONSES);
    }

    @Test
    void ueaDeactivatedAndNoQuotaAndCupoExceeded() {
        TrimestralPlanGroup group = TrimestralPlanGroup.createProposed(
                plan, 40L, (short) 1, "2156041", "METODOS", "OBLIGATORIA", "CO43", "1");
        group.addStudent(GroupStudent.create(
                group, 101L, "2123001", "Ana Lopez", StudentSource.SURVEY, "I", (short) 1));
        group.addStudent(GroupStudent.create(
                group, 102L, "2123002", "Bruno Diaz", StudentSource.SURVEY, "I", (short) 2));
        plan.replaceGroups(List.of(group));

        List<PlanWarning> warnings = engine.evaluate(
                plan,
                new WarningContext(
                        false,
                        Set.of(),
                        Set.of(40L),
                        Set.of(101L, 102L),
                        Set.of()));

        assertThat(warnings).extracting(PlanWarning::getCode)
                .contains(WarningCode.UEA_DEACTIVATED, WarningCode.UEA_NO_QUOTA, WarningCode.CUPO_EXCEEDED);
    }

    @Test
    void inactiveStudentAndProfessor() {
        TrimestralPlanGroup group = TrimestralPlanGroup.createEdited(
                plan,
                40L,
                (short) 1,
                "2156041",
                "METODOS",
                "OBLIGATORIA",
                "CO43",
                "25",
                8L,
                "12345",
                "Prof X",
                null,
                emptySchedule());
        group.addStudent(GroupStudent.create(
                group, 101L, "2123001", "Ana Lopez", StudentSource.SURVEY, "I", (short) 1));
        plan.replaceGroups(List.of(group));

        List<PlanWarning> warnings = engine.evaluate(
                plan,
                new WarningContext(false, Set.of(40L), Set.of(), Set.of(), Set.of()));

        assertThat(warnings).extracting(PlanWarning::getCode)
                .contains(WarningCode.STUDENT_INACTIVE, WarningCode.PROFESSOR_INACTIVE);
    }

    private static java.util.Map<mx.uam.sapcyti.trimestral.domain.model.ScheduleDay, TrimestralPlanGroup.DaySlot>
            emptySchedule() {
        java.util.EnumMap<mx.uam.sapcyti.trimestral.domain.model.ScheduleDay, TrimestralPlanGroup.DaySlot> map =
                new java.util.EnumMap<>(mx.uam.sapcyti.trimestral.domain.model.ScheduleDay.class);
        for (mx.uam.sapcyti.trimestral.domain.model.ScheduleDay day :
                mx.uam.sapcyti.trimestral.domain.model.ScheduleDay.values()) {
            map.put(day, new TrimestralPlanGroup.DaySlot(null, null, false));
        }
        return map;
    }
}

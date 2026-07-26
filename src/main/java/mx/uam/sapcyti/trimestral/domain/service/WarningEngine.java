package mx.uam.sapcyti.trimestral.domain.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.PlanWarning;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.WarningCode;
import org.springframework.stereotype.Service;

/**
 * Recalculates the full warning set for a plan on every write (SPEC-035).
 */
@Service
public class WarningEngine {

    public List<PlanWarning> evaluate(TrimestralPlan plan, WarningContext ctx) {
        List<PlanWarning> warnings = new ArrayList<>();

        if (ctx.noEnrollResponses()) {
            warnings.add(PlanWarning.of(plan, WarningCode.NO_RESPONSES, null, null, null, null));
        }

        Set<Long> seenInactiveStudents = new HashSet<>();
        Set<String> seenDeactivatedClaves = new HashSet<>();

        for (TrimestralPlanGroup group : plan.getGroups()) {
            if (!ctx.activeUeaIds().contains(group.getUeaId())
                    && seenDeactivatedClaves.add(group.getClave())) {
                warnings.add(PlanWarning.of(
                        plan, WarningCode.UEA_DEACTIVATED, group.getClave(), null, null, null));
            }

            for (GroupStudent student : group.getStudents()) {
                if (!ctx.activeStudentIds().contains(student.getStudentId())
                        && seenInactiveStudents.add(student.getStudentId())) {
                    warnings.add(PlanWarning.of(
                            plan,
                            WarningCode.STUDENT_INACTIVE,
                            null,
                            student.getEnrollmentId(),
                            null,
                            null));
                }
            }

            // A group may carry co-directors: warn once per inactive professor.
            for (var professor : group.getProfessors()) {
                if (!ctx.activeProfessorIds().contains(professor.getProfessorId())) {
                    warnings.add(PlanWarning.of(
                            plan,
                            WarningCode.PROFESSOR_INACTIVE,
                            null,
                            null,
                            professor.getEmployeeNumber(),
                            group.getId()));
                }
            }

            if (group.exceedsCupo()) {
                warnings.add(PlanWarning.of(
                        plan, WarningCode.CUPO_EXCEEDED, null, null, null, group.getId()));
            }
        }

        return warnings;
    }

    public record WarningContext(
            boolean noEnrollResponses,
            Set<Long> activeUeaIds,
            Set<Long> activeStudentIds,
            Set<Long> activeProfessorIds) {

        public static WarningContext empty() {
            return new WarningContext(false, Set.of(), Set.of(), Set.of());
        }

        public WarningContext {
            activeUeaIds = activeUeaIds == null ? Set.of() : Set.copyOf(activeUeaIds);
            activeStudentIds = activeStudentIds == null ? Set.of() : Set.copyOf(activeStudentIds);
            activeProfessorIds = activeProfessorIds == null ? Set.of() : Set.copyOf(activeProfessorIds);
        }
    }
}

package mx.uam.sapcyti.trimestral.domain.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        Set<String> seenNoQuotaClaves = new HashSet<>();

        for (TrimestralPlanGroup group : plan.getGroups()) {
            if (!ctx.activeUeaIds().contains(group.getUeaId())
                    && seenDeactivatedClaves.add(group.getClave())) {
                warnings.add(PlanWarning.of(
                        plan, WarningCode.UEA_DEACTIVATED, group.getClave(), null, null, null));
            }

            if (ctx.ueasWithoutQuota().contains(group.getUeaId())
                    && seenNoQuotaClaves.add(group.getClave())) {
                warnings.add(PlanWarning.of(
                        plan, WarningCode.UEA_NO_QUOTA, group.getClave(), null, null, null));
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

            if (group.getProfessorId() != null
                    && !ctx.activeProfessorIds().contains(group.getProfessorId())) {
                warnings.add(PlanWarning.of(
                        plan,
                        WarningCode.PROFESSOR_INACTIVE,
                        null,
                        null,
                        group.getEmployeeNumber(),
                        group.getId()));
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
            Set<Long> ueasWithoutQuota,
            Set<Long> activeStudentIds,
            Set<Long> activeProfessorIds) {

        public static WarningContext empty() {
            return new WarningContext(false, Set.of(), Set.of(), Set.of(), Set.of());
        }

        public WarningContext {
            activeUeaIds = activeUeaIds == null ? Set.of() : Set.copyOf(activeUeaIds);
            ueasWithoutQuota = ueasWithoutQuota == null ? Set.of() : Set.copyOf(ueasWithoutQuota);
            activeStudentIds = activeStudentIds == null ? Set.of() : Set.copyOf(activeStudentIds);
            activeProfessorIds = activeProfessorIds == null ? Set.of() : Set.copyOf(activeProfessorIds);
        }

        public static Set<Long> ueasWithoutQuotaFrom(
                Map<Long, String> cupoByUeaId, Set<Long> demandedUeaIds) {
            Set<Long> missing = new HashSet<>();
            for (Long ueaId : demandedUeaIds) {
                String cupo = cupoByUeaId.get(ueaId);
                if (cupo == null || cupo.isBlank()) {
                    missing.add(ueaId);
                }
            }
            return missing;
        }
    }
}

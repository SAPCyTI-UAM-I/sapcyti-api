package mx.uam.sapcyti.trimestral.application.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.StudentSource;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.UnassignedDemand;
import mx.uam.sapcyti.trimestral.domain.model.UnassignedDemandReason;
import mx.uam.sapcyti.trimestral.domain.service.GroupLetterService;
import org.springframework.stereotype.Component;

/**
 * Applies annual quota limits to one UEA's globally prioritized survey demand.
 */
@Component
@RequiredArgsConstructor
class TrimestralDemandAllocator {

    private static final int MAX_SUFFIX_INDEX = 26;

    private final GroupLetterService groupLetterService;

    short allocate(
            TrimestralPlan plan,
            UEA uea,
            TrimestralQuota quota,
            List<DemandStudent> rawDemand,
            List<TrimestralPlanGroup> groups,
            List<UnassignedDemand> unassigned,
            short initialPosition) {
        List<DemandStudent> demand = rawDemand.stream()
                .sorted(DEMAND_PRIORITY)
                .toList();
        if (quota == null || !quota.isOffered()) {
            addUnassigned(plan, uea, demand, UnassignedDemandReason.UEA_NOT_OFFERED, unassigned);
            return initialPosition;
        }

        Integer capacity = quota.finiteCapacity();
        Integer maximumGroups = quota.finiteMaximumGroups();
        int usedGroups = 0;
        short position = initialPosition;
        Map<String, BaseAllocation> allocationByBase = new LinkedHashMap<>();

        // Consume the global priority one student at a time. Research demand can
        // interleave several bases while the maximum remains global to the UEA.
        for (DemandStudent demandStudent : demand) {
            String base = baseFor(uea, demandStudent.academicTerm());
            BaseAllocation allocation =
                    allocationByBase.computeIfAbsent(base, ignored -> new BaseAllocation());
            TrimestralPlanGroup group = allocation.currentGroup;
            boolean hasCapacity =
                    group != null && (capacity == null || group.getStudents().size() < capacity);
            if (!hasCapacity) {
                if (maximumGroups != null && usedGroups >= maximumGroups) {
                    addUnassigned(
                            plan,
                            uea,
                            List.of(demandStudent),
                            UnassignedDemandReason.GROUP_LIMIT_REACHED,
                            unassigned);
                    continue;
                }
                if (allocation.nextSuffixIndex > MAX_SUFFIX_INDEX) {
                    addUnassigned(
                            plan,
                            uea,
                            List.of(demandStudent),
                            UnassignedDemandReason.GROUP_SUFFIX_LIMIT,
                            unassigned);
                    continue;
                }
                group = TrimestralPlanGroup.createProposed(
                        plan,
                        uea.getId(),
                        position++,
                        uea.getClave(),
                        uea.getNombre(),
                        uea.getTipo().name(),
                        groupCode(base, allocation.nextSuffixIndex++),
                        quota.capacity(),
                        quota.maxGroups());
                allocation.currentGroup = group;
                groups.add(group);
                usedGroups++;
            }
            group.addStudent(toGroupStudent(
                    group, demandStudent, (short) (group.getStudents().size() + 1)));
        }
        return position;
    }

    private String baseFor(UEA uea, AcademicTerm academicTerm) {
        if (uea.getTipoFormacion() != FormationType.INVESTIGACION) {
            return groupLetterService.baseGroup('O');
        }
        int termNumber = academicTerm.ordinal() + 1;
        return groupLetterService
                .letterForTerm(termNumber)
                .map(groupLetterService::baseGroup)
                .orElse(null);
    }

    private static String groupCode(String base, int suffixIndex) {
        if (base == null || suffixIndex == 0) {
            return base;
        }
        return base + (char) ('A' + suffixIndex - 1);
    }

    private static GroupStudent toGroupStudent(
            TrimestralPlanGroup group, DemandStudent demand, short position) {
        Student student = demand.student();
        return GroupStudent.create(
                group,
                student.getId(),
                student.getEnrollmentId(),
                PersonSnapshotFormatter.fullName(student.getPersonalData()),
                StudentSource.SURVEY,
                demand.academicTerm().name(),
                null,
                position);
    }

    private static void addUnassigned(
            TrimestralPlan plan,
            UEA uea,
            List<DemandStudent> students,
            UnassignedDemandReason reason,
            List<UnassignedDemand> target) {
        for (DemandStudent demand : students) {
            Student student = demand.student();
            target.add(UnassignedDemand.create(
                    plan,
                    uea.getId(),
                    uea.getClave(),
                    uea.getNombre(),
                    student.getId(),
                    student.getEnrollmentId(),
                    PersonSnapshotFormatter.fullName(student.getPersonalData()),
                    demand.academicTerm().name(),
                    reason,
                    (short) (target.size() + 1)));
        }
    }

    static final Comparator<DemandStudent> DEMAND_PRIORITY =
            (left, right) -> StudentPriority.compare(left.student(), right.student());

    record DemandStudent(Student student, AcademicTerm academicTerm) {
    }

    private static final class BaseAllocation {
        private TrimestralPlanGroup currentGroup;
        private int nextSuffixIndex;
    }
}

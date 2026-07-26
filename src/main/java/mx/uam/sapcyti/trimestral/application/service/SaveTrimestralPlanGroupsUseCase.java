package mx.uam.sapcyti.trimestral.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotFoundException;
import mx.uam.sapcyti.trimestral.domain.model.PlanWarning;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.UnassignedDemand;
import mx.uam.sapcyti.trimestral.domain.model.UnassignedDemandReason;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import mx.uam.sapcyti.trimestral.domain.service.WarningEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveTrimestralPlanGroupsUseCase {

    private final TrimestralPlanRepositoryPort planRepository;
    private final UeaRepositoryPort ueaRepository;
    private final StudentRepositoryPort studentRepository;
    private final TrimestralGroupRebuilder groupRebuilder;
    private final TrimestralPlanGenerationSupport generationSupport;
    private final WarningEngine warningEngine;
    private final TrimestralPrerequisiteGuard prerequisiteGuard;

    @Transactional
    public TrimestralPlan execute(Long planId, List<TrimestralGroupInput> groups) {
        Long graduateProgramId = requireTenant();
        TrimestralPlan plan = planRepository
                .findByIdAndGraduateProgramId(planId, graduateProgramId)
                .orElseThrow(TrimestralPlanNotFoundException::new);

        prerequisiteGuard.assertSatisfied(plan);
        plan.assertEditable();

        Map<Long, Student> studentsById = studentRepository.findByGraduateProgramId(graduateProgramId).stream()
                .collect(Collectors.toMap(Student::getId, student -> student));
        Map<SurveyDemandKey, AcademicTerm> surveyDemand =
                generationSupport.surveyDemand(plan.getSurveyId());
        Map<Long, TrimestralQuota> quotaByUea = generationSupport.quotaLimits(plan);
        Map<Long, TrimestralPlanGroup> existingById = plan.getGroups().stream()
                .filter(group -> group.getId() != null)
                .collect(Collectors.toMap(TrimestralPlanGroup::getId, group -> group));
        Map<SurveyDemandKey, UnassignedDemand> existingUnassigned =
                plan.getUnassignedDemand().stream()
                        .collect(Collectors.toMap(
                                demand -> new SurveyDemandKey(
                                        demand.getStudentId(), demand.getUeaId()),
                                demand -> demand));

        List<TrimestralPlanGroup> rebuilt = groupRebuilder.rebuild(
                groups,
                plan,
                graduateProgramId,
                studentsById,
                surveyDemand,
                quotaByUea,
                existingById);
        List<UnassignedDemand> reconciledUnassigned = reconcileUnassignedDemand(
                plan,
                rebuilt,
                surveyDemand,
                existingUnassigned,
                quotaByUea,
                studentsById,
                graduateProgramId);

        // Manual save keeps outdated reasons; regeneration is the operation that clears them.
        plan.replaceGroups(rebuilt);
        plan.replaceUnassignedDemand(reconciledUnassigned);
        TrimestralPlan saved = planRepository.save(plan);
        List<PlanWarning> warnings =
                warningEngine.evaluate(saved, generationSupport.buildWarningContextForPlan(saved));
        saved.replaceWarnings(warnings);
        return planRepository.save(saved);
    }

    private List<UnassignedDemand> reconcileUnassignedDemand(
            TrimestralPlan plan,
            List<TrimestralPlanGroup> groups,
            Map<SurveyDemandKey, AcademicTerm> surveyDemand,
            Map<SurveyDemandKey, UnassignedDemand> existingUnassigned,
            Map<Long, TrimestralQuota> quotaByUea,
            Map<Long, Student> studentsById,
            Long graduateProgramId) {
        Set<SurveyDemandKey> assigned = groups.stream()
                .flatMap(group -> group.getStudents().stream()
                        .map(student ->
                                new SurveyDemandKey(student.getStudentId(), group.getUeaId())))
                .collect(Collectors.toSet());
        List<UnassignedDemand> result = new ArrayList<>();
        List<Map.Entry<SurveyDemandKey, AcademicTerm>> ordered =
                surveyDemand.entrySet().stream()
                        .sorted((left, right) -> StudentPriority.compare(
                                studentsById.get(left.getKey().studentId()),
                                studentsById.get(right.getKey().studentId())))
                        .toList();

        for (Map.Entry<SurveyDemandKey, AcademicTerm> entry : ordered) {
            SurveyDemandKey key = entry.getKey();
            if (assigned.contains(key)) {
                continue;
            }
            Student student = studentsById.get(key.studentId());
            UEA uea = ueaRepository
                    .findByIdAndGraduateProgramId(key.ueaId(), graduateProgramId)
                    .orElse(null);
            if (student == null || uea == null) {
                continue;
            }
            UnassignedDemand previous = existingUnassigned.get(key);
            UnassignedDemandReason reason =
                    unassignedReason(quotaByUea.get(key.ueaId()), previous);
            result.add(UnassignedDemand.create(
                    plan,
                    uea.getId(),
                    uea.getClave(),
                    uea.getNombre(),
                    student.getId(),
                    student.getEnrollmentId(),
                    PersonSnapshotFormatter.fullName(student.getPersonalData()),
                    entry.getValue().name(),
                    reason,
                    (short) (result.size() + 1)));
        }
        return result;
    }

    private static UnassignedDemandReason unassignedReason(
            TrimestralQuota quota, UnassignedDemand previous) {
        if (quota == null || !quota.isOffered()) {
            return UnassignedDemandReason.UEA_NOT_OFFERED;
        }
        if (previous != null
                && previous.getReason() != UnassignedDemandReason.MANUALLY_UNASSIGNED) {
            return previous.getReason();
        }
        return UnassignedDemandReason.MANUALLY_UNASSIGNED;
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }
}

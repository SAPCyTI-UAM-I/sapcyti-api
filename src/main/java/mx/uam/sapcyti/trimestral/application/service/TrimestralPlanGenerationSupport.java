package mx.uam.sapcyti.trimestral.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanQuota;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.PlanWarning;
import mx.uam.sapcyti.trimestral.domain.model.StudentSource;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.UnassignedDemand;
import mx.uam.sapcyti.trimestral.domain.model.UnassignedDemandReason;
import mx.uam.sapcyti.trimestral.domain.service.GroupLetterService;
import mx.uam.sapcyti.trimestral.domain.service.WarningEngine;
import mx.uam.sapcyti.trimestral.domain.service.WarningEngine.WarningContext;
import org.springframework.stereotype.Component;

/**
 * Shared generation logic for create and regenerate (HU-58).
 */
@Component
@RequiredArgsConstructor
public class TrimestralPlanGenerationSupport {

    private final SurveyResponseRepositoryPort surveyResponseRepository;
    private final AnnualPlanRepositoryPort annualPlanRepository;
    private final UeaRepositoryPort ueaRepository;
    private final StudentRepositoryPort studentRepository;
    private final ProfessorRepositoryPort professorRepository;
    private final UserRepositoryPort userRepository;
    private final GroupLetterService groupLetterService;
    private final WarningEngine warningEngine;

    public void populateFromSurvey(TrimestralPlan plan) {
        Long gpId = plan.getGraduateProgramId();
        String term = plan.getTerm();
        int year = TrimestralPlan.yearFromTerm(term);
        char trimester = TrimestralPlan.trimesterLetter(term);

        List<StudentSurveyResponse> responses = surveyResponseRepository.findAllBySurveyId(plan.getSurveyId());
        Map<Long, Student> studentsById = studentRepository.findByGraduateProgramId(gpId).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        Map<Long, QuotaLimits> quotaByUeaId = new HashMap<>();
        for (AnnualPlanQuota quota : annualPlanRepository.findQuotas(year, gpId)) {
            quotaByUeaId.put(
                    quota.ueaId(),
                    new QuotaLimits(
                            blankToNull(quota.groupsForTrimester(trimester)),
                            blankToNull(quota.cupoForTrimester(trimester))));
        }

        Map<Long, UEA> ueasById = new HashMap<>();
        Map<Long, List<DemandStudent>> demandByUea = new LinkedHashMap<>();

        for (StudentSurveyResponse response : responses) {
            if (response.getMode() != SurveyResponseMode.ENROLL_UEAS) {
                continue;
            }
            Student student = studentsById.get(response.getStudentId());
            if (student == null) {
                continue;
            }
            for (Long ueaId : response.getUeaIds()) {
                demandByUea
                        .computeIfAbsent(ueaId, ignored -> new ArrayList<>())
                        .add(new DemandStudent(student, response.getAcademicTerm()));
                ueasById.computeIfAbsent(ueaId, id -> ueaRepository
                        .findByIdAndGraduateProgramId(id, gpId)
                        .orElse(null));
            }
        }

        List<TrimestralPlanGroup> groups = new ArrayList<>();
        List<UnassignedDemand> unassigned = new ArrayList<>();
        short position = 1;
        for (Map.Entry<Long, List<DemandStudent>> entry : demandByUea.entrySet()) {
            Long ueaId = entry.getKey();
            UEA uea = ueasById.get(ueaId);
            if (uea == null) {
                continue;
            }
            List<DemandStudent> demand = entry.getValue().stream()
                    .sorted(demandPriority())
                    .toList();

            QuotaLimits quota = quotaByUeaId.get(ueaId);
            if (quota == null || quota.maxGroups() == null || quota.capacity() == null) {
                addUnassigned(plan, uea, demand, UnassignedDemandReason.UEA_NOT_OFFERED, unassigned);
                continue;
            }

            GenerationPosition result = addGroups(
                    plan, uea, quota, demand, groups, unassigned, position);
            position = result.nextPosition();
        }

        plan.replaceGroups(groups);
        plan.replaceUnassignedDemand(unassigned);
    }

    public void refreshWarnings(TrimestralPlan plan) {
        List<PlanWarning> warnings = warningEngine.evaluate(plan, buildWarningContextForPlan(plan));
        plan.replaceWarnings(warnings);
    }

    public WarningContext buildWarningContext(
            TrimestralPlan plan,
            boolean noEnrollResponses,
            Set<Long> activeUeaIds,
            Set<Long> demandedOrGroupedUeaIds,
            Map<Long, String> cupoByUeaId,
            Map<Long, Integer> maxGroupsByUeaId,
            Map<Long, Student> studentsById) {
        Set<Long> ueasWithoutQuota =
                WarningContext.ueasWithoutQuotaFrom(cupoByUeaId, demandedOrGroupedUeaIds);

        Set<Long> activeStudentIds = new HashSet<>();
        for (Student student : studentsById.values()) {
            if (isUserActive(student.getUserId())) {
                activeStudentIds.add(student.getId());
            }
        }

        Set<Long> activeProfessorIds = new HashSet<>();
        for (TrimestralPlanGroup group : plan.getGroups()) {
            for (var groupProfessor : group.getProfessors()) {
                Optional<Professor> professor = professorRepository.findByIdAndGraduateProgramId(
                        groupProfessor.getProfessorId(), plan.getGraduateProgramId());
                if (professor.isPresent() && isUserActive(professor.get().getUserId())) {
                    activeProfessorIds.add(professor.get().getId());
                }
            }
        }

        return new WarningContext(
                noEnrollResponses,
                activeUeaIds,
                ueasWithoutQuota,
                activeStudentIds,
                activeProfessorIds,
                maxGroupsByUeaId);
    }

    public WarningContext buildWarningContextForPlan(TrimestralPlan plan) {
        Long gpId = plan.getGraduateProgramId();
        int year = TrimestralPlan.yearFromTerm(plan.getTerm());
        char trimester = TrimestralPlan.trimesterLetter(plan.getTerm());

        Map<Long, String> cupoByUeaId = new HashMap<>();
        Map<Long, Integer> maxGroupsByUeaId = new HashMap<>();
        for (AnnualPlanQuota quota : annualPlanRepository.findQuotas(year, gpId)) {
            cupoByUeaId.put(quota.ueaId(), blankToNull(quota.cupoForTrimester(trimester)));
            Integer maxGroups = positiveIntegerOrNull(quota.groupsForTrimester(trimester));
            if (maxGroups != null) {
                maxGroupsByUeaId.put(quota.ueaId(), maxGroups);
            }
        }

        Set<Long> activeUeaIds = ueaRepository.findActiveByGraduateProgramId(gpId).stream()
                .map(UEA::getId)
                .collect(Collectors.toSet());

        Set<Long> groupedUeaIds = plan.getGroups().stream()
                .map(TrimestralPlanGroup::getUeaId)
                .collect(Collectors.toSet());

        Map<Long, Student> studentsById = studentRepository.findByGraduateProgramId(gpId).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        List<StudentSurveyResponse> responses = surveyResponseRepository.findAllBySurveyId(plan.getSurveyId());
        boolean hasEnroll = responses.stream().anyMatch(r -> r.getMode() == SurveyResponseMode.ENROLL_UEAS);

        return buildWarningContext(
                plan,
                !hasEnroll,
                activeUeaIds,
                groupedUeaIds,
                cupoByUeaId,
                maxGroupsByUeaId,
                studentsById);
    }

    public List<BlankStudentView> deriveBlankStudents(TrimestralPlan plan) {
        Map<Long, Student> studentsById = studentRepository
                .findByGraduateProgramId(plan.getGraduateProgramId())
                .stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Set<Long> assigned = plan.assignedStudentIds();

        List<BlankStudentView> blanks = new ArrayList<>();
        for (StudentSurveyResponse response :
                surveyResponseRepository.findAllBySurveyId(plan.getSurveyId())) {
            if (response.getMode() != SurveyResponseMode.BLANK) {
                continue;
            }
            if (assigned.contains(response.getStudentId())) {
                continue;
            }
            Student student = studentsById.get(response.getStudentId());
            if (student == null) {
                continue;
            }
            blanks.add(new BlankStudentView(
                    student.getId(),
                    student.getEnrollmentId(),
                    formatFullName(student.getPersonalData()),
                    response.getAcademicTerm().name()));
        }
        blanks.sort(Comparator.comparing(BlankStudentView::enrollmentId));
        return blanks;
    }

    public Map<DemandKey, AcademicTerm> surveyDemand(Long surveyId) {
        Map<DemandKey, AcademicTerm> demand = new HashMap<>();
        for (StudentSurveyResponse response : surveyResponseRepository.findAllBySurveyId(surveyId)) {
            if (response.getMode() != SurveyResponseMode.ENROLL_UEAS) {
                continue;
            }
            for (Long ueaId : response.getUeaIds()) {
                demand.put(new DemandKey(response.getStudentId(), ueaId), response.getAcademicTerm());
            }
        }
        return Map.copyOf(demand);
    }

    private GenerationPosition addGroups(
            TrimestralPlan plan,
            UEA uea,
            QuotaLimits quota,
            List<DemandStudent> demand,
            List<TrimestralPlanGroup> groups,
            List<UnassignedDemand> unassigned,
            short position) {
        Integer capacity = positiveIntegerOrNull(quota.capacity());
        Integer maxGroups = positiveIntegerOrNull(quota.maxGroups());
        int usedGroups = 0;
        Map<String, BaseAllocation> allocationByBase = new LinkedHashMap<>();

        // Consume the globally sorted demand one student at a time. This is important
        // when research demand interleaves several bases and the annual group maximum
        // is global to the UEA.
        for (DemandStudent student : demand) {
            String base = uea.getTipoFormacion() == FormationType.INVESTIGACION
                    ? proposedBase(student.academicTerm())
                    : groupLetterService.baseGroup('O');
            BaseAllocation allocation =
                    allocationByBase.computeIfAbsent(base, ignored -> new BaseAllocation());
            TrimestralPlanGroup group = allocation.currentGroup;
            boolean hasCapacity =
                    group != null && (capacity == null || group.getStudents().size() < capacity);
            if (!hasCapacity) {
                if (maxGroups != null && usedGroups >= maxGroups) {
                    addUnassigned(
                            plan,
                            uea,
                            List.of(student),
                            UnassignedDemandReason.GROUP_LIMIT_REACHED,
                            unassigned);
                    continue;
                }
                if (allocation.nextSuffixIndex > 26) {
                    addUnassigned(
                            plan,
                            uea,
                            List.of(student),
                            UnassignedDemandReason.GROUP_SUFFIX_LIMIT,
                            unassigned);
                    continue;
                }
                group = proposedGroup(
                        plan,
                        uea,
                        position++,
                        groupCode(base, allocation.nextSuffixIndex++),
                        quota.capacity(),
                        quota.maxGroups());
                allocation.currentGroup = group;
                groups.add(group);
                usedGroups++;
            }
            group.addStudent(toGroupStudent(
                    group, student, (short) (group.getStudents().size() + 1)));
        }
        return new GenerationPosition(position);
    }

    private static TrimestralPlanGroup proposedGroup(
            TrimestralPlan plan,
            UEA uea,
            short position,
            String groupCode,
            String cupo,
            String maxGroups) {
        return TrimestralPlanGroup.createProposed(
                plan,
                uea.getId(),
                position,
                uea.getClave(),
                uea.getNombre(),
                uea.getTipo().name(),
                groupCode,
                cupo,
                maxGroups);
    }

    private static String groupCode(String base, int index) {
        if (base == null || index == 0) {
            return base;
        }
        return base + (char) ('A' + index - 1);
    }

    private static Integer positiveIntegerOrNull(String value) {
        if (value == null || value.isBlank() || "*".equals(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String proposedBase(AcademicTerm academicTerm) {
        int n = academicTerm.ordinal() + 1;
        return groupLetterService.letterForTerm(n).map(groupLetterService::baseGroup).orElse(null);
    }

    private static GroupStudent toGroupStudent(TrimestralPlanGroup group, DemandStudent d, short posicion) {
        Student student = d.student();
        return GroupStudent.create(
                group,
                student.getId(),
                student.getEnrollmentId(),
                formatFullName(student.getPersonalData()),
                StudentSource.SURVEY,
                d.academicTerm().name(),
                null,
                posicion);
    }

    private boolean isUserActive(Long userId) {
        return userRepository.findById(userId).map(User::isActive).orElse(false);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static String formatFullName(PersonalData personalData) {
        String second = personalData.getSecondLastName();
        if (second == null || second.isBlank()) {
            return personalData.getFirstName() + " " + personalData.getFirstLastName();
        }
        return personalData.getFirstName() + " " + personalData.getFirstLastName() + " " + second;
    }

    public static String formatProfessorName(PersonalData personalData) {
        return formatFullName(personalData);
    }

    private record DemandStudent(Student student, AcademicTerm academicTerm) {
    }

    public Map<Long, QuotaLimits> quotaLimits(TrimestralPlan plan) {
        int year = TrimestralPlan.yearFromTerm(plan.getTerm());
        char trimester = TrimestralPlan.trimesterLetter(plan.getTerm());
        return annualPlanRepository.findQuotas(year, plan.getGraduateProgramId()).stream()
                .collect(Collectors.toMap(
                        AnnualPlanQuota::ueaId,
                        quota -> new QuotaLimits(
                                blankToNull(quota.groupsForTrimester(trimester)),
                                blankToNull(quota.cupoForTrimester(trimester)))));
    }

    private static Comparator<DemandStudent> demandPriority() {
        return Comparator.comparing(
                        (DemandStudent d) -> d.student().getPersonalData().getFirstLastName(),
                        Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(
                        d -> d.student().getPersonalData().getSecondLastName(),
                        Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(
                        d -> d.student().getPersonalData().getFirstName(),
                        Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(d -> d.student().getEnrollmentId(), Comparator.nullsLast(String::compareTo));
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
                    formatFullName(student.getPersonalData()),
                    demand.academicTerm().name(),
                    reason,
                    (short) (target.size() + 1)));
        }
    }

    private record GenerationPosition(short nextPosition) {}

    private static final class BaseAllocation {
        private TrimestralPlanGroup currentGroup;
        private int nextSuffixIndex;
    }

    public record QuotaLimits(String maxGroups, String capacity) {}

    public record DemandKey(Long studentId, Long ueaId) {}

    public record BlankStudentView(Long studentId, String enrollmentId, String fullName, String academicTerm) {
    }
}

package mx.uam.sapcyti.trimestral.application.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.offering.domain.exception.UeaNotFoundException;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.trimestral.application.service.TrimestralPlanGenerationSupport.DemandKey;
import mx.uam.sapcyti.trimestral.application.service.TrimestralPlanGenerationSupport.QuotaLimits;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotFoundException;
import mx.uam.sapcyti.trimestral.domain.model.GroupProfessor;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.PlanWarning;
import mx.uam.sapcyti.trimestral.domain.model.ScheduleDay;
import mx.uam.sapcyti.trimestral.domain.model.StudentSource;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup.DaySlot;
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
    private final ProfessorRepositoryPort professorRepository;
    private final UserRepositoryPort userRepository;
    private final TrimestralPlanGenerationSupport generationSupport;
    private final WarningEngine warningEngine;
    private final TrimestralPrerequisiteGuard prerequisiteGuard;

    @Transactional
    public TrimestralPlan execute(Long planId, List<GroupInput> groups) {
        Long graduateProgramId = requireTenant();
        TrimestralPlan plan = planRepository
                .findByIdAndGraduateProgramId(planId, graduateProgramId)
                .orElseThrow(TrimestralPlanNotFoundException::new);

        prerequisiteGuard.assertSatisfied(plan);
        plan.assertEditable();

        Map<Long, Student> studentsById = studentRepository.findByGraduateProgramId(graduateProgramId).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Set<Long> tenantStudentIds = studentsById.keySet();
        Map<DemandKey, AcademicTerm> surveyDemand = generationSupport.surveyDemand(plan.getSurveyId());
        Map<Long, QuotaLimits> quotaByUea = generationSupport.quotaLimits(plan);

        // Preserve existing snapshots when group id is reused
        Map<Long, TrimestralPlanGroup> existingById = plan.getGroups().stream()
                .filter(g -> g.getId() != null)
                .collect(Collectors.toMap(TrimestralPlanGroup::getId, g -> g));
        Map<DemandKey, UnassignedDemand> existingUnassigned = plan.getUnassignedDemand().stream()
                .collect(Collectors.toMap(
                        demand -> new DemandKey(demand.getStudentId(), demand.getUeaId()),
                        demand -> demand));

        List<TrimestralPlanGroup> rebuilt = new ArrayList<>();
        short position = 1;
        // A student normally appears in several groups because the survey allows several
        // UEAs. What is invalid is assigning the same student twice to the same UEA (for
        // example, to two suffixed groups of a cupo-1 research course).
        Map<Long, Set<Long>> seenStudentIdsByUea = new HashMap<>();
        Map<Long, Integer> groupCountByUea = new HashMap<>();

        for (GroupInput input : groups) {
            TrimestralPlanGroup.validateGrupo(input.grupo());
            TrimestralPlanGroup.validateCupo(input.cupo());
            Map<ScheduleDay, DaySlot> schedule = toScheduleMap(input.schedule());
            for (DaySlot slot : schedule.values()) {
                TrimestralPlanGroup.validateSlot(slot);
            }

            UEA uea = ueaRepository
                    .findByIdAndGraduateProgramId(input.ueaId(), graduateProgramId)
                    .orElseThrow(UeaNotFoundException::new);
            QuotaLimits quota = quotaByUea.get(input.ueaId());
            if (quota == null || quota.maxGroups() == null || quota.capacity() == null) {
                throw new IllegalArgumentException("ueaId " + input.ueaId() + " is not offered in the annual plan");
            }
            if (!quota.capacity().equals(input.cupo())) {
                throw new IllegalArgumentException(
                        "cupo for ueaId " + input.ueaId() + " must match the annual plan");
            }
            int groupCount = groupCountByUea.merge(input.ueaId(), 1, Integer::sum);
            Integer maximumGroups = positiveIntegerOrNull(quota.maxGroups());
            if (maximumGroups != null && groupCount > maximumGroups) {
                throw new IllegalArgumentException(
                        "groups for ueaId " + input.ueaId() + " exceed the annual plan maximum");
            }

            String clave = uea.getClave();
            String nombre = uea.getNombre();
            String tipoUea = uea.getTipo().name();
            TrimestralPlanGroup existing = null;
            if (input.id() != null) {
                existing = existingById.get(input.id());
                if (existing != null && existing.getUeaId().equals(input.ueaId())) {
                    clave = existing.getClave();
                    nombre = existing.getNombre();
                    tipoUea = existing.getTipoUea();
                } else {
                    existing = null;
                }
            }

            TrimestralPlanGroup group = TrimestralPlanGroup.createEdited(
                    plan,
                    input.ueaId(),
                    position++,
                    clave,
                    nombre,
                    tipoUea,
                    input.grupo(),
                    input.cupo(),
                    quota.maxGroups(),
                    schedule);

            // Research groups have co-directors: resolve each professor into a snapshot,
            // preserving the captured order. Duplicates within the same group are rejected.
            List<GroupProfessor> groupProfessors = new ArrayList<>();
            Set<Long> seenProfessorIds = new HashSet<>();
            Map<Long, GroupProfessor> existingProfessors = existing == null
                    ? Map.of()
                    : existing.getProfessors().stream()
                            .collect(Collectors.toMap(GroupProfessor::getProfessorId, professor -> professor));
            short professorPos = 1;
            for (Long professorId : input.professorIds()) {
                if (professorId == null) {
                    throw new IllegalArgumentException("professorId is required");
                }
                if (!seenProfessorIds.add(professorId)) {
                    throw new IllegalArgumentException(
                            "professorId " + professorId + " is repeated in the same group");
                }
                Professor professor = professorRepository
                        .findByIdAndGraduateProgramId(professorId, graduateProgramId)
                        .orElseThrow(ProfessorNotFoundException::new);
                if (!isUserActive(professor.getUserId())) {
                    GroupProfessor previous = existingProfessors.get(professorId);
                    if (previous == null) {
                        throw new ProfessorNotFoundException();
                    }
                    groupProfessors.add(GroupProfessor.create(
                            group,
                            professorId,
                            previous.getEmployeeNumber(),
                            previous.getProfessorName(),
                            professorPos++));
                    continue;
                }
                groupProfessors.add(GroupProfessor.create(
                        group,
                        professorId,
                        professor.getEmployeeNumber(),
                        TrimestralPlanGenerationSupport.formatProfessorName(professor.getPersonalData()),
                        professorPos++));
            }
            group.replaceProfessors(groupProfessors);

            List<GroupStudent> members = new ArrayList<>();
            short studentPos = 1;
            Set<Long> seenStudentIds = seenStudentIdsByUea.computeIfAbsent(
                    input.ueaId(), ignored -> new HashSet<>());
            List<StudentInput> orderedStudents = new ArrayList<>(input.students());
            orderedStudents.sort((a, b) -> {
                Student sa = studentsById.get(a.studentId());
                Student sb = studentsById.get(b.studentId());
                if (sa == null || sb == null) {
                    return 0;
                }
                return TrimestralPlanGroup.surnameComparator()
                        .compare(toSnapshot(sa, StudentSource.SURVEY, null), toSnapshot(sb, StudentSource.SURVEY, null));
            });

            for (StudentInput studentInput : orderedStudents) {
                Long studentId = studentInput.studentId();
                if (studentId == null) {
                    throw new IllegalArgumentException("studentId is required");
                }
                GroupStudent.validateObs(studentInput.obs());
                if (!tenantStudentIds.contains(studentId)) {
                    throw new StudentNotFoundException();
                }
                if (!seenStudentIds.add(studentId)) {
                    throw new IllegalArgumentException(
                            "studentId " + studentId + " appears more than once for ueaId " + input.ueaId());
                }
                Student student = studentsById.get(studentId);
                if (!isUserActive(student.getUserId())) {
                    GroupStudent previous = existing == null
                            ? null
                            : existing.getStudents().stream()
                                    .filter(member -> member.getStudentId().equals(studentId))
                                    .findFirst()
                                    .orElse(null);
                    if (previous == null) {
                        throw new StudentNotFoundException();
                    }
                    members.add(GroupStudent.create(
                            group,
                            studentId,
                            previous.getEnrollmentId(),
                            previous.getFullName(),
                            previous.getSource(),
                            previous.getAcademicTerm(),
                            studentInput.obs(),
                            studentPos++));
                    continue;
                }

                AcademicTerm surveyTerm = surveyDemand.get(new DemandKey(studentId, input.ueaId()));
                StudentSource source = surveyTerm != null ? StudentSource.SURVEY : StudentSource.MANUAL;
                String academicTerm = source == StudentSource.SURVEY ? surveyTerm.name() : null;

                members.add(GroupStudent.create(
                        group,
                        studentId,
                        student.getEnrollmentId(),
                        TrimestralPlanGenerationSupport.formatFullName(student.getPersonalData()),
                        source,
                        academicTerm,
                        studentInput.obs(),
                        studentPos++));
            }
            Integer capacity = positiveIntegerOrNull(input.cupo());
            if (capacity != null && members.size() > capacity) {
                throw new IllegalArgumentException(
                        "students in group " + input.grupo() + " exceed cupo " + input.cupo());
            }
            group.replaceStudents(members);
            rebuilt.add(group);
        }

        List<UnassignedDemand> reconciledUnassigned = reconcileUnassignedDemand(
                plan,
                rebuilt,
                surveyDemand,
                existingUnassigned,
                quotaByUea,
                studentsById,
                graduateProgramId);

        // outdated reasons are intentionally preserved on manual save
        plan.replaceGroups(rebuilt);
        plan.replaceUnassignedDemand(reconciledUnassigned);
        TrimestralPlan saved = planRepository.save(plan);
        List<PlanWarning> warnings =
                warningEngine.evaluate(saved, generationSupport.buildWarningContextForPlan(saved));
        saved.replaceWarnings(warnings);
        return planRepository.save(saved);
    }

    private static Map<ScheduleDay, DaySlot> toScheduleMap(List<DayScheduleInput> schedule) {
        if (schedule == null || schedule.size() != 5) {
            throw new IllegalArgumentException("schedule must contain exactly 5 days LUN..VIE");
        }
        Map<ScheduleDay, DaySlot> map = new EnumMap<>(ScheduleDay.class);
        for (DayScheduleInput day : schedule) {
            if (day == null || day.day() == null) {
                throw new IllegalArgumentException("schedule day is required");
            }
            if (map.containsKey(day.day())) {
                throw new IllegalArgumentException("schedule day " + day.day() + " is duplicated");
            }
            map.put(day.day(), new DaySlot(day.start(), day.end(), day.lab()));
        }
        return map;
    }

    private static TrimestralPlanGroup.GroupStudentSnapshot toSnapshot(
            Student student, StudentSource source, String academicTerm) {
        return new TrimestralPlanGroup.GroupStudentSnapshot(
                student.getId(),
                student.getEnrollmentId(),
                TrimestralPlanGenerationSupport.formatFullName(student.getPersonalData()),
                student.getPersonalData().getFirstName(),
                student.getPersonalData().getFirstLastName(),
                student.getPersonalData().getSecondLastName(),
                source,
                academicTerm);
    }

    private boolean isUserActive(Long userId) {
        return userRepository.findById(userId).map(User::isActive).orElse(false);
    }

    private List<UnassignedDemand> reconcileUnassignedDemand(
            TrimestralPlan plan,
            List<TrimestralPlanGroup> groups,
            Map<DemandKey, AcademicTerm> surveyDemand,
            Map<DemandKey, UnassignedDemand> existingUnassigned,
            Map<Long, QuotaLimits> quotaByUea,
            Map<Long, Student> studentsById,
            Long graduateProgramId) {
        Set<DemandKey> assigned = groups.stream()
                .flatMap(group -> group.getStudents().stream()
                        .map(student -> new DemandKey(student.getStudentId(), group.getUeaId())))
                .collect(Collectors.toSet());
        List<UnassignedDemand> result = new ArrayList<>();
        List<Map.Entry<DemandKey, AcademicTerm>> ordered = surveyDemand.entrySet().stream()
                .sorted((left, right) -> {
                    Student a = studentsById.get(left.getKey().studentId());
                    Student b = studentsById.get(right.getKey().studentId());
                    if (a == null || b == null) {
                        return 0;
                    }
                    return TrimestralPlanGroup.surnameComparator()
                            .compare(toSnapshot(a, StudentSource.SURVEY, null), toSnapshot(b, StudentSource.SURVEY, null));
                })
                .toList();
        for (Map.Entry<DemandKey, AcademicTerm> entry : ordered) {
            DemandKey key = entry.getKey();
            if (assigned.contains(key)) {
                continue;
            }
            Student student = studentsById.get(key.studentId());
            if (student == null) {
                continue;
            }
            UEA uea = ueaRepository
                    .findByIdAndGraduateProgramId(key.ueaId(), graduateProgramId)
                    .orElse(null);
            if (uea == null) {
                continue;
            }
            UnassignedDemand previous = existingUnassigned.get(key);
            QuotaLimits quota = quotaByUea.get(key.ueaId());
            UnassignedDemandReason reason;
            if (quota == null || quota.maxGroups() == null || quota.capacity() == null) {
                reason = UnassignedDemandReason.UEA_NOT_OFFERED;
            } else if (previous != null
                    && previous.getReason() != UnassignedDemandReason.MANUALLY_UNASSIGNED) {
                reason = previous.getReason();
            } else {
                reason = UnassignedDemandReason.MANUALLY_UNASSIGNED;
            }
            result.add(UnassignedDemand.create(
                    plan,
                    uea.getId(),
                    uea.getClave(),
                    uea.getNombre(),
                    student.getId(),
                    student.getEnrollmentId(),
                    TrimestralPlanGenerationSupport.formatFullName(student.getPersonalData()),
                    entry.getValue().name(),
                    reason,
                    (short) (result.size() + 1)));
        }
        return result;
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

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }

    public record GroupInput(
            Long id,
            Long ueaId,
            String grupo,
            String cupo,
            List<Long> professorIds,
            List<DayScheduleInput> schedule,
            List<StudentInput> students) {

        public GroupInput {
            professorIds = professorIds == null ? List.of() : List.copyOf(professorIds);
            schedule = schedule == null ? null : List.copyOf(schedule);
            students = students == null ? List.of() : List.copyOf(students);
        }
    }

    public record StudentInput(Long studentId, String obs) {
    }

    public record DayScheduleInput(ScheduleDay day, String start, String end, boolean lab) {
    }
}

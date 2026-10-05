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
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.offering.domain.exception.UeaNotFoundException;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.trimestral.domain.model.GroupProfessor;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.ScheduleDay;
import mx.uam.sapcyti.trimestral.domain.model.StudentSource;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup.DaySlot;
import org.springframework.stereotype.Component;

/**
 * Rebuilds editable groups from a save command while preserving valid inactive snapshots.
 */
@Component
@RequiredArgsConstructor
class TrimestralGroupRebuilder {

    private final UeaRepositoryPort ueaRepository;
    private final ProfessorRepositoryPort professorRepository;
    private final UserRepositoryPort userRepository;

    List<TrimestralPlanGroup> rebuild(
            List<TrimestralGroupInput> inputs,
            TrimestralPlan plan,
            Long graduateProgramId,
            Map<Long, Student> studentsById,
            Map<SurveyDemandKey, AcademicTerm> surveyDemand,
            Map<Long, TrimestralQuota> quotaByUea,
            Map<Long, TrimestralPlanGroup> existingById) {
        RebuildContext context = new RebuildContext(
                plan,
                graduateProgramId,
                studentsById,
                surveyDemand,
                quotaByUea,
                existingById,
                new HashMap<>(),
                new HashMap<>());
        List<TrimestralPlanGroup> rebuilt = new ArrayList<>();
        for (TrimestralGroupInput input : inputs) {
            rebuilt.add(rebuildGroup(input, (short) (rebuilt.size() + 1), context));
        }
        return rebuilt;
    }

    private TrimestralPlanGroup rebuildGroup(
            TrimestralGroupInput input, short position, RebuildContext context) {
        TrimestralPlanGroup.validateGrupo(input.grupo());
        TrimestralPlanGroup.validateCupo(input.cupo());
        Map<ScheduleDay, DaySlot> schedule = toScheduleMap(input.schedule());
        schedule.values().forEach(TrimestralPlanGroup::validateSlot);

        UEA uea = ueaRepository
                .findByIdAndGraduateProgramId(input.ueaId(), context.graduateProgramId())
                .orElseThrow(UeaNotFoundException::new);
        TrimestralQuota quota = requireOfferedQuota(input.ueaId(), input.cupo(), context);
        TrimestralPlanGroup existing = matchingExistingGroup(input, context.existingById());
        GroupSnapshot snapshot = existing == null
                ? new GroupSnapshot(uea.getClave(), uea.getNombre(), uea.getTipo().name())
                : new GroupSnapshot(existing.getClave(), existing.getNombre(), existing.getTipoUea());

        TrimestralPlanGroup group = TrimestralPlanGroup.createEdited(
                context.plan(),
                input.ueaId(),
                position,
                snapshot.clave(),
                snapshot.nombre(),
                snapshot.tipoUea(),
                input.grupo(),
                quota.capacity(),
                quota.maxGroups(),
                schedule);
        group.replaceProfessors(resolveProfessors(group, input.professorIds(), existing, context.graduateProgramId()));
        group.replaceStudents(resolveStudents(group, input, existing, context));
        return group;
    }

    private static TrimestralPlanGroup matchingExistingGroup(
            TrimestralGroupInput input, Map<Long, TrimestralPlanGroup> existingById) {
        if (input.id() == null) {
            return null;
        }
        TrimestralPlanGroup existing = existingById.get(input.id());
        return existing != null && existing.getUeaId().equals(input.ueaId()) ? existing : null;
    }

    private static TrimestralQuota requireOfferedQuota(
            Long ueaId, String requestedCapacity, RebuildContext context) {
        TrimestralQuota quota = context.quotaByUea().get(ueaId);
        if (quota == null || !quota.isOffered()) {
            throw new IllegalArgumentException("ueaId " + ueaId + " is not offered in the annual plan");
        }
        String resolvedCapacity = requestedCapacity == null ? quota.capacity() : requestedCapacity;
        if (!quota.capacity().equals(resolvedCapacity)) {
            throw new IllegalArgumentException("cupo for ueaId " + ueaId + " must match the annual plan");
        }
        int groupCount = context.groupCountByUea().merge(ueaId, 1, Integer::sum);
        Integer maximumGroups = quota.finiteMaximumGroups();
        if (maximumGroups != null && groupCount > maximumGroups) {
            throw new IllegalArgumentException(
                    "groups for ueaId " + ueaId + " exceed the annual plan maximum");
        }
        return quota;
    }

    private List<GroupProfessor> resolveProfessors(
            TrimestralPlanGroup group,
            List<Long> professorIds,
            TrimestralPlanGroup existing,
            Long graduateProgramId) {
        Map<Long, GroupProfessor> existingProfessors = existing == null
                ? Map.of()
                : existing.getProfessors().stream()
                        .collect(Collectors.toMap(GroupProfessor::getProfessorId, professor -> professor));
        List<GroupProfessor> result = new ArrayList<>();
        Set<Long> seenProfessorIds = new HashSet<>();
        for (Long professorId : professorIds) {
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
            GroupProfessor previous = existingProfessors.get(professorId);
            boolean active = isUserActive(professor.getUserId());
            if (!active && previous == null) {
                throw new ProfessorNotFoundException();
            }
            result.add(GroupProfessor.create(
                    group,
                    professorId,
                    active ? professor.getEmployeeNumber() : previous.getEmployeeNumber(),
                    active
                            ? PersonSnapshotFormatter.fullName(professor.getPersonalData())
                            : previous.getProfessorName(),
                    (short) (result.size() + 1)));
        }
        return result;
    }

    private List<GroupStudent> resolveStudents(
            TrimestralPlanGroup group,
            TrimestralGroupInput input,
            TrimestralPlanGroup existing,
            RebuildContext context) {
        Set<Long> seenStudentIds = context.seenStudentIdsByUea()
                .computeIfAbsent(input.ueaId(), ignored -> new HashSet<>());
        List<TrimestralGroupInput.Student> orderedStudents = new ArrayList<>(input.students());
        orderedStudents.sort((left, right) ->
                compareStudents(context.studentsById().get(left.studentId()), context.studentsById().get(right.studentId())));

        List<GroupStudent> members = new ArrayList<>();
        for (TrimestralGroupInput.Student studentInput : orderedStudents) {
            members.add(resolveStudent(
                    group,
                    input.ueaId(),
                    studentInput,
                    existing,
                    context,
                    seenStudentIds,
                    members.size()));
        }
        TrimestralQuota quota = context.quotaByUea().get(input.ueaId());
        Integer capacity = quota.finiteCapacity();
        if (capacity != null && members.size() > capacity) {
            throw new IllegalArgumentException(
                    "students in group " + input.grupo() + " exceed cupo " + quota.capacity());
        }
        return members;
    }

    private GroupStudent resolveStudent(
            TrimestralPlanGroup group,
            Long ueaId,
            TrimestralGroupInput.Student input,
            TrimestralPlanGroup existing,
            RebuildContext context,
            Set<Long> seenStudentIds,
            int currentSize) {
        Long studentId = input.studentId();
        if (studentId == null) {
            throw new IllegalArgumentException("studentId is required");
        }
        GroupStudent.validateObs(input.obs());
        Student student = context.studentsById().get(studentId);
        if (student == null) {
            throw new StudentNotFoundException();
        }
        if (!seenStudentIds.add(studentId)) {
            throw new IllegalArgumentException(
                    "studentId " + studentId + " appears more than once for ueaId " + ueaId);
        }
        GroupStudent previous = findExistingStudent(existing, studentId);
        if (!isUserActive(student.getUserId())) {
            if (previous == null) {
                throw new StudentNotFoundException();
            }
            return GroupStudent.create(
                    group,
                    studentId,
                    previous.getEnrollmentId(),
                    previous.getFullName(),
                    previous.getSource(),
                    previous.getAcademicTerm(),
                    input.obs(),
                    (short) (currentSize + 1));
        }

        AcademicTerm surveyTerm = context.surveyDemand().get(new SurveyDemandKey(studentId, ueaId));
        StudentSource source = surveyTerm != null ? StudentSource.SURVEY : StudentSource.MANUAL;
        return GroupStudent.create(
                group,
                studentId,
                student.getEnrollmentId(),
                PersonSnapshotFormatter.fullName(student.getPersonalData()),
                source,
                surveyTerm == null ? null : surveyTerm.name(),
                input.obs(),
                (short) (currentSize + 1));
    }

    private static GroupStudent findExistingStudent(TrimestralPlanGroup existing, Long studentId) {
        if (existing == null) {
            return null;
        }
        return existing.getStudents().stream()
                .filter(member -> member.getStudentId().equals(studentId))
                .findFirst()
                .orElse(null);
    }

    private static int compareStudents(Student left, Student right) {
        return StudentPriority.compare(left, right);
    }

    private boolean isUserActive(Long userId) {
        return userRepository.findById(userId).map(User::isActive).orElse(false);
    }

    private static Map<ScheduleDay, DaySlot> toScheduleMap(
            List<TrimestralGroupInput.DaySchedule> schedule) {
        if (schedule == null || schedule.size() != 5) {
            throw new IllegalArgumentException("schedule must contain exactly 5 days LUN..VIE");
        }
        Map<ScheduleDay, DaySlot> map = new EnumMap<>(ScheduleDay.class);
        for (TrimestralGroupInput.DaySchedule day : schedule) {
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

    private record RebuildContext(
            TrimestralPlan plan,
            Long graduateProgramId,
            Map<Long, Student> studentsById,
            Map<SurveyDemandKey, AcademicTerm> surveyDemand,
            Map<Long, TrimestralQuota> quotaByUea,
            Map<Long, TrimestralPlanGroup> existingById,
            Map<Long, Set<Long>> seenStudentIdsByUea,
            Map<Long, Integer> groupCountByUea) {
    }

    private record GroupSnapshot(String clave, String nombre, String tipoUea) {
    }
}

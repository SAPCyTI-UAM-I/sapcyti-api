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
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotFoundException;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.PlanWarning;
import mx.uam.sapcyti.trimestral.domain.model.ScheduleDay;
import mx.uam.sapcyti.trimestral.domain.model.StudentSource;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup.DaySlot;
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

    @Transactional
    public TrimestralPlan execute(Long planId, List<GroupInput> groups) {
        Long graduateProgramId = requireTenant();
        TrimestralPlan plan = planRepository
                .findByIdAndGraduateProgramId(planId, graduateProgramId)
                .orElseThrow(TrimestralPlanNotFoundException::new);

        // Editable check BEFORE format validation (api-spec order)
        plan.assertEditable();

        Map<Long, Student> studentsById = studentRepository.findByGraduateProgramId(graduateProgramId).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Set<Long> tenantStudentIds = studentsById.keySet();
        Map<Long, AcademicTerm> surveyTerms = generationSupport.surveyAcademicTerms(plan.getSurveyId());

        // Preserve existing snapshots when group id is reused
        Map<Long, TrimestralPlanGroup> existingById = plan.getGroups().stream()
                .filter(g -> g.getId() != null)
                .collect(Collectors.toMap(TrimestralPlanGroup::getId, g -> g));

        List<TrimestralPlanGroup> rebuilt = new ArrayList<>();
        short position = 1;
        Set<Long> seenStudentIds = new HashSet<>();

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

            String clave = uea.getClave();
            String nombre = uea.getNombre();
            String tipoUea = uea.getTipo().name();
            if (input.id() != null) {
                TrimestralPlanGroup existing = existingById.get(input.id());
                if (existing != null && existing.getUeaId().equals(input.ueaId())) {
                    clave = existing.getClave();
                    nombre = existing.getNombre();
                    tipoUea = existing.getTipoUea();
                }
            }

            Long professorId = input.professorId();
            String employeeNumber = null;
            String professorName = null;
            if (professorId != null) {
                Professor professor = professorRepository
                        .findByIdAndGraduateProgramId(professorId, graduateProgramId)
                        .orElseThrow(ProfessorNotFoundException::new);
                if (!isUserActive(professor.getUserId())) {
                    throw new ProfessorNotFoundException();
                }
                employeeNumber = professor.getEmployeeNumber();
                professorName = TrimestralPlanGenerationSupport.formatProfessorName(professor.getPersonalData());
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
                    professorId,
                    employeeNumber,
                    professorName,
                    schedule);

            List<GroupStudent> members = new ArrayList<>();
            short studentPos = 1;
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
                    throw new IllegalArgumentException("studentId " + studentId + " appears in more than one group");
                }
                Student student = studentsById.get(studentId);
                if (!isUserActive(student.getUserId())) {
                    throw new StudentNotFoundException();
                }

                AcademicTerm surveyTerm = surveyTerms.get(studentId);
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
            group.replaceStudents(members);
            rebuilt.add(group);
        }

        // outdated is preserved on manual save
        plan.replaceGroups(rebuilt);
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
            Long professorId,
            List<DayScheduleInput> schedule,
            List<StudentInput> students) {
    }

    public record StudentInput(Long studentId, String obs) {
    }

    public record DayScheduleInput(ScheduleDay day, String start, String end, boolean lab) {
    }
}

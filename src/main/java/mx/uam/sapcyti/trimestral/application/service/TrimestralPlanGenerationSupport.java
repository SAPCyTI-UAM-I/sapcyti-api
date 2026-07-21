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

        Map<Long, String> cupoByUeaId = new HashMap<>();
        for (AnnualPlanQuota quota : annualPlanRepository.findQuotas(year, gpId)) {
            cupoByUeaId.put(quota.ueaId(), blankToNull(quota.cupoForTrimester(trimester)));
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
        short position = 1;
        for (Map.Entry<Long, List<DemandStudent>> entry : demandByUea.entrySet()) {
            Long ueaId = entry.getKey();
            UEA uea = ueasById.get(ueaId);
            if (uea == null) {
                continue;
            }
            String cupo = cupoByUeaId.get(ueaId);
            List<DemandStudent> demand = entry.getValue().stream()
                    .sorted(Comparator.comparing(
                                    (DemandStudent d) -> d.student().getPersonalData().getFirstLastName(),
                                    Comparator.nullsLast(String::compareToIgnoreCase))
                            .thenComparing(
                                    d -> d.student().getPersonalData().getSecondLastName(),
                                    Comparator.nullsLast(String::compareToIgnoreCase))
                            .thenComparing(
                                    d -> d.student().getPersonalData().getFirstName(),
                                    Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();

            if (isCupoOne(cupo) && demand.size() > 1) {
                Map<String, List<DemandStudent>> byBase = new LinkedHashMap<>();
                List<DemandStudent> withoutLetter = new ArrayList<>();
                for (DemandStudent d : demand) {
                    String base = proposedBase(d.academicTerm());
                    if (base == null) {
                        withoutLetter.add(d);
                    } else {
                        byBase.computeIfAbsent(base, ignored -> new ArrayList<>()).add(d);
                    }
                }
                for (Map.Entry<String, List<DemandStudent>> baseEntry : byBase.entrySet()) {
                    String base = baseEntry.getKey();
                    List<DemandStudent> sameBase = baseEntry.getValue();
                    List<GroupLetterService.Group> letterGroups = sameBase.stream()
                            .map(d -> {
                                PersonalData pd = d.student().getPersonalData();
                                return new GroupLetterService.Group(
                                        base, pd.getFirstLastName(), pd.getSecondLastName(), pd.getFirstName());
                            })
                            .toList();
                    List<String> suffixed = groupLetterService.assignSuffixes(letterGroups);
                    List<DemandStudent> sortedSame = sameBase.stream()
                            .sorted(Comparator.comparing(
                                            (DemandStudent d) -> d.student().getPersonalData().getFirstLastName(),
                                            Comparator.nullsLast(String::compareToIgnoreCase))
                                    .thenComparing(
                                            d -> d.student().getPersonalData().getSecondLastName(),
                                            Comparator.nullsLast(String::compareToIgnoreCase))
                                    .thenComparing(
                                            d -> d.student().getPersonalData().getFirstName(),
                                            Comparator.nullsLast(String::compareToIgnoreCase)))
                            .toList();
                    for (int i = 0; i < sortedSame.size(); i++) {
                        DemandStudent d = sortedSame.get(i);
                        TrimestralPlanGroup group = TrimestralPlanGroup.createProposed(
                                plan,
                                ueaId,
                                position++,
                                uea.getClave(),
                                uea.getNombre(),
                                uea.getTipo().name(),
                                suffixed.get(i),
                                cupo);
                        group.addStudent(toGroupStudent(group, d, (short) 1));
                        groups.add(group);
                    }
                }
                for (DemandStudent d : withoutLetter) {
                    TrimestralPlanGroup group = TrimestralPlanGroup.createProposed(
                            plan,
                            ueaId,
                            position++,
                            uea.getClave(),
                            uea.getNombre(),
                            uea.getTipo().name(),
                            null,
                            cupo);
                    group.addStudent(toGroupStudent(group, d, (short) 1));
                    groups.add(group);
                }
            } else {
                String grupo = demand.isEmpty() ? null : proposedBase(demand.getFirst().academicTerm());
                TrimestralPlanGroup group = TrimestralPlanGroup.createProposed(
                        plan,
                        ueaId,
                        position++,
                        uea.getClave(),
                        uea.getNombre(),
                        uea.getTipo().name(),
                        grupo,
                        cupo);
                short studentPos = 1;
                for (DemandStudent d : demand) {
                    group.addStudent(toGroupStudent(group, d, studentPos++));
                }
                groups.add(group);
            }
        }

        plan.replaceGroups(groups);
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
                noEnrollResponses, activeUeaIds, ueasWithoutQuota, activeStudentIds, activeProfessorIds);
    }

    public WarningContext buildWarningContextForPlan(TrimestralPlan plan) {
        Long gpId = plan.getGraduateProgramId();
        int year = TrimestralPlan.yearFromTerm(plan.getTerm());
        char trimester = TrimestralPlan.trimesterLetter(plan.getTerm());

        Map<Long, String> cupoByUeaId = new HashMap<>();
        for (AnnualPlanQuota quota : annualPlanRepository.findQuotas(year, gpId)) {
            cupoByUeaId.put(quota.ueaId(), blankToNull(quota.cupoForTrimester(trimester)));
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

        return buildWarningContext(plan, !hasEnroll, activeUeaIds, groupedUeaIds, cupoByUeaId, studentsById);
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

    public Map<Long, AcademicTerm> surveyAcademicTerms(Long surveyId) {
        Map<Long, AcademicTerm> map = new HashMap<>();
        for (StudentSurveyResponse response : surveyResponseRepository.findAllBySurveyId(surveyId)) {
            map.put(response.getStudentId(), response.getAcademicTerm());
        }
        return map;
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

    private static boolean isCupoOne(String cupo) {
        return "1".equals(cupo);
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

    public record BlankStudentView(Long studentId, String enrollmentId, String fullName, String academicTerm) {
    }
}

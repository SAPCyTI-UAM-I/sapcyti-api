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
import mx.uam.sapcyti.trimestral.domain.model.PlanWarning;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.UnassignedDemand;
import mx.uam.sapcyti.trimestral.application.service.TrimestralDemandAllocator.DemandStudent;
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
    private final TrimestralDemandAllocator demandAllocator;
    private final WarningEngine warningEngine;

    public void populateFromSurvey(TrimestralPlan plan) {
        Long gpId = plan.getGraduateProgramId();
        String term = plan.getTerm();
        int year = TrimestralPlan.yearFromTerm(term);
        char trimester = TrimestralPlan.trimesterLetter(term);

        List<StudentSurveyResponse> responses = surveyResponseRepository.findAllBySurveyId(plan.getSurveyId());
        Map<Long, Student> studentsById = studentRepository.findByGraduateProgramId(gpId).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        Map<Long, TrimestralQuota> quotaByUeaId = new HashMap<>();
        for (AnnualPlanQuota quota : annualPlanRepository.findQuotas(year, gpId)) {
            quotaByUeaId.put(
                    quota.ueaId(),
                    new TrimestralQuota(
                            quota.groupsForTrimester(trimester),
                            quota.cupoForTrimester(trimester)));
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
            position = demandAllocator.allocate(
                    plan,
                    uea,
                    quotaByUeaId.get(ueaId),
                    entry.getValue(),
                    groups,
                    unassigned,
                    position);
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
            Map<Long, Student> studentsById) {
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
                activeStudentIds,
                activeProfessorIds);
    }

    public WarningContext buildWarningContextForPlan(TrimestralPlan plan) {
        Long gpId = plan.getGraduateProgramId();

        Set<Long> activeUeaIds = ueaRepository.findActiveByGraduateProgramId(gpId).stream()
                .map(UEA::getId)
                .collect(Collectors.toSet());

        Map<Long, Student> studentsById = studentRepository.findByGraduateProgramId(gpId).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        List<StudentSurveyResponse> responses = surveyResponseRepository.findAllBySurveyId(plan.getSurveyId());
        boolean hasEnroll = responses.stream().anyMatch(r -> r.getMode() == SurveyResponseMode.ENROLL_UEAS);

        return buildWarningContext(
                plan,
                !hasEnroll,
                activeUeaIds,
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
                    PersonSnapshotFormatter.fullName(student.getPersonalData()),
                    response.getAcademicTerm().name()));
        }
        blanks.sort(Comparator.comparing(BlankStudentView::enrollmentId));
        return blanks;
    }

    public Map<SurveyDemandKey, AcademicTerm> surveyDemand(Long surveyId) {
        Map<SurveyDemandKey, AcademicTerm> demand = new HashMap<>();
        for (StudentSurveyResponse response : surveyResponseRepository.findAllBySurveyId(surveyId)) {
            if (response.getMode() != SurveyResponseMode.ENROLL_UEAS) {
                continue;
            }
            for (Long ueaId : response.getUeaIds()) {
                demand.put(new SurveyDemandKey(response.getStudentId(), ueaId), response.getAcademicTerm());
            }
        }
        return Map.copyOf(demand);
    }

    private boolean isUserActive(Long userId) {
        return userRepository.findById(userId).map(User::isActive).orElse(false);
    }

    public Map<Long, TrimestralQuota> quotaLimits(TrimestralPlan plan) {
        int year = TrimestralPlan.yearFromTerm(plan.getTerm());
        char trimester = TrimestralPlan.trimesterLetter(plan.getTerm());
        return annualPlanRepository.findQuotas(year, plan.getGraduateProgramId()).stream()
                .collect(Collectors.toMap(
                        AnnualPlanQuota::ueaId,
                        quota -> new TrimestralQuota(
                                quota.groupsForTrimester(trimester),
                                quota.cupoForTrimester(trimester))));
    }

    public record BlankStudentView(Long studentId, String enrollmentId, String fullName, String academicTerm) {
    }
}

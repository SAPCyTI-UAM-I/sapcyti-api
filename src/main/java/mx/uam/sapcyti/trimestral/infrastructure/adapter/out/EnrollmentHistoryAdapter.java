package mx.uam.sapcyti.trimestral.infrastructure.adapter.out;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.DaySchedule;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.EnrollmentHistoryEntry;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.EnrollmentHistoryUea;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.HistoryProfessor;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.HistoryPlanStatus;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.HistoryUeaStatus;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import mx.uam.sapcyti.trimestral.domain.model.GroupStudent;
import mx.uam.sapcyti.trimestral.domain.model.ScheduleDay;
import mx.uam.sapcyti.trimestral.domain.model.StudentSource;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup.DaySlot;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanStatus;
import mx.uam.sapcyti.trimestral.domain.model.UnassignedDemand;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EnrollmentHistoryAdapter implements EnrollmentHistoryPort {

    private final SurveyResponseRepositoryPort surveyResponseRepository;
    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final TrimestralPlanRepositoryPort planRepository;
    private final UeaRepositoryPort ueaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentHistoryEntry> findByStudent(Long studentId, Long graduateProgramId) {
        Map<String, StudentSurveyResponse> responseByTerm = new HashMap<>();
        for (StudentSurveyResponse response :
                surveyResponseRepository.findAllByStudentIdAndGraduateProgramId(studentId, graduateProgramId)) {
            surveyRepository
                    .findByIdAndGraduateProgramId(response.getSurveyId(), graduateProgramId)
                    .ifPresent(survey -> responseByTerm.put(survey.getTerm(), response));
        }

        Map<String, TrimestralPlan> terminadaPlanByTerm = new HashMap<>();
        for (TrimestralPlan plan : planRepository.findAllByGraduateProgramIdAndStudentId(
                graduateProgramId, studentId)) {
            if (plan.getStatus() == TrimestralPlanStatus.TERMINADA) {
                terminadaPlanByTerm.put(plan.getTerm(), plan);
            }
        }

        Set<String> terms = new HashSet<>();
        terms.addAll(responseByTerm.keySet());
        terms.addAll(terminadaPlanByTerm.keySet());

        List<EnrollmentHistoryEntry> entries = new ArrayList<>();
        for (String term : terms) {
            TrimestralPlan terminadaPlan = terminadaPlanByTerm.get(term);
            if (terminadaPlan == null) {
                terminadaPlan = planRepository
                        .findByTermAndGraduateProgramId(term, graduateProgramId)
                        .filter(plan -> plan.getStatus() == TrimestralPlanStatus.TERMINADA)
                        .orElse(null);
            }
            StudentSurveyResponse response = responseByTerm.get(term);
            if (terminadaPlan != null) {
                entries.add(buildTerminadaEntry(term, terminadaPlan, response, studentId, graduateProgramId));
            } else if (response != null) {
                entries.add(buildPendingEntry(term, response, graduateProgramId));
            }
        }

        entries.sort(Comparator.comparing(EnrollmentHistoryEntry::term, TrimestralPlan.termNewestFirst()));
        return entries;
    }

    private EnrollmentHistoryEntry buildPendingEntry(
            String term, StudentSurveyResponse response, Long graduateProgramId) {
        List<EnrollmentHistoryUea> ueas = List.of();
        if (response.getMode() == SurveyResponseMode.ENROLL_UEAS) {
            ueas = response.getUeaIds().stream()
                    .map(ueaId -> pendingUea(ueaId, graduateProgramId))
                    .flatMap(Optional::stream)
                    .toList();
        }
        return new EnrollmentHistoryEntry(
                term,
                response.getAcademicTerm().name(),
                response.getMode().name(),
                HistoryPlanStatus.PENDING,
                "PENDING",
                ueas);
    }

    private EnrollmentHistoryEntry buildTerminadaEntry(
            String term,
            TrimestralPlan plan,
            StudentSurveyResponse response,
            Long studentId,
            Long graduateProgramId) {
        boolean manualWithoutSurvey = response == null && isManualStudent(plan, studentId);
        String academicTermSelected = response == null ? null : response.getAcademicTerm().name();
        String mode = response == null ? null : response.getMode().name();
        String note = manualWithoutSurvey ? "MANUAL_NOT_SURVEYED" : null;

        List<EnrollmentHistoryUea> ueas;
        if (response != null && response.getMode() == SurveyResponseMode.BLANK) {
            ueas = List.of();
        } else {
            Map<Long, EnrollmentHistoryUea> assignedByUea = new java.util.LinkedHashMap<>();
            for (TrimestralPlanGroup group : groupsForStudent(plan, studentId)) {
                assignedByUea.put(group.getUeaId(), terminadaUea(group));
            }
            List<EnrollmentHistoryUea> finalUeas = new ArrayList<>();
            if (response != null && response.getMode() == SurveyResponseMode.ENROLL_UEAS) {
                for (Long requestedUeaId : response.getUeaIds()) {
                    EnrollmentHistoryUea assigned = assignedByUea.remove(requestedUeaId);
                    if (assigned != null) {
                        finalUeas.add(assigned);
                    } else {
                        removedUea(plan, studentId, requestedUeaId, graduateProgramId)
                                .ifPresent(finalUeas::add);
                    }
                }
            }
            finalUeas.addAll(assignedByUea.values());
            ueas = List.copyOf(finalUeas);
        }

        return new EnrollmentHistoryEntry(
                term, academicTermSelected, mode, HistoryPlanStatus.TERMINADA, note, ueas);
    }

    private static boolean isManualStudent(TrimestralPlan plan, Long studentId) {
        return groupsForStudent(plan, studentId).stream()
                .flatMap(group -> group.getStudents().stream())
                .anyMatch(student -> student.getStudentId().equals(studentId)
                        && student.getSource() == StudentSource.MANUAL);
    }

    private static List<TrimestralPlanGroup> groupsForStudent(TrimestralPlan plan, Long studentId) {
        return plan.getGroups().stream()
                .filter(group -> group.getStudents().stream()
                        .anyMatch(student -> student.getStudentId().equals(studentId)))
                .toList();
    }

    private EnrollmentHistoryUea terminadaUea(TrimestralPlanGroup group) {
        List<DaySchedule> schedule = new ArrayList<>();
        ScheduleDay[] days = ScheduleDay.values();
        List<DaySlot> slots = group.scheduleInOrder();
        for (int i = 0; i < days.length; i++) {
            DaySlot slot = slots.get(i);
            schedule.add(new DaySchedule(days[i].name(), slot.start(), slot.end(), slot.lab()));
        }
        List<HistoryProfessor> professors = group.getProfessors().stream()
                .map(professor -> new HistoryProfessor(
                        professor.getProfessorId(),
                        professor.getEmployeeNumber(),
                        professor.getProfessorName()))
                .toList();
        return new EnrollmentHistoryUea(
                HistoryUeaStatus.ASSIGNED,
                group.getClave(),
                group.getNombre(),
                group.getGrupo(),
                professors,
                schedule);
    }

    private Optional<EnrollmentHistoryUea> pendingUea(Long ueaId, Long graduateProgramId) {
        return ueaRepository
                .findByIdAndGraduateProgramId(ueaId, graduateProgramId)
                .map(this::pendingUeaFromCatalog);
    }

    private EnrollmentHistoryUea pendingUeaFromCatalog(UEA uea) {
        return new EnrollmentHistoryUea(
                HistoryUeaStatus.PENDING,
                uea.getClave(),
                uea.getNombre(),
                null,
                List.of(),
                null);
    }

    private Optional<EnrollmentHistoryUea> removedUea(
            TrimestralPlan plan,
            Long studentId,
            Long ueaId,
            Long graduateProgramId) {
        Optional<UnassignedDemand> snapshot = plan.getUnassignedDemand().stream()
                .filter(item -> item.getStudentId().equals(studentId) && item.getUeaId().equals(ueaId))
                .findFirst();
        if (snapshot.isPresent()) {
            UnassignedDemand demand = snapshot.orElseThrow();
            return Optional.of(new EnrollmentHistoryUea(
                    HistoryUeaStatus.REMOVED_FROM_FINAL_PLAN,
                    demand.getClave(),
                    demand.getNombre(),
                    null,
                    List.of(),
                    null));
        }
        return ueaRepository.findByIdAndGraduateProgramId(ueaId, graduateProgramId)
                .map(uea -> new EnrollmentHistoryUea(
                        HistoryUeaStatus.REMOVED_FROM_FINAL_PLAN,
                        uea.getClave(),
                        uea.getNombre(),
                        null,
                        List.of(),
                        null));
    }
}

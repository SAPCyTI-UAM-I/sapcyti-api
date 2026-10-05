package mx.uam.sapcyti.trimestral.infrastructure.adapter.out;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorTrimestralAssignmentsPort;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanStatus;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProfessorTrimestralAssignmentsAdapter implements ProfessorTrimestralAssignmentsPort {

    private final TrimestralPlanRepositoryPort planRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OpenGroupAssignment> findOpenGroupAssignments(
            Long professorId, Long graduateProgramId) {
        List<OpenGroupAssignment> assignments = new ArrayList<>();
        for (var plan : planRepository.findAllByGraduateProgramId(graduateProgramId)) {
            if (plan.getStatus() == TrimestralPlanStatus.TERMINADA) {
                continue;
            }
            for (var group : plan.getGroups()) {
                boolean assigned = group.getProfessors().stream()
                        .anyMatch(professor -> professor.getProfessorId().equals(professorId));
                if (assigned) {
                    assignments.add(new OpenGroupAssignment(
                            plan.getId(),
                            plan.getTerm(),
                            group.getUeaId(),
                            group.getClave(),
                            group.getGrupo()));
                }
            }
        }
        return List.copyOf(assignments);
    }

    @Override
    @Transactional
    public void refreshOpenPlanSnapshots(
            Long professorId,
            Long graduateProgramId,
            String employeeNumber,
            String fullName) {
        for (var plan : planRepository.findAllByGraduateProgramId(graduateProgramId)) {
            if (plan.getStatus() == TrimestralPlanStatus.TERMINADA) {
                continue;
            }
            boolean changed = false;
            for (var group : plan.getGroups()) {
                for (var professor : group.getProfessors()) {
                    if (professor.getProfessorId().equals(professorId)) {
                        professor.refreshSnapshot(employeeNumber, fullName);
                        changed = true;
                    }
                }
            }
            if (changed) {
                plan.touch();
                planRepository.save(plan);
            }
        }
    }
}

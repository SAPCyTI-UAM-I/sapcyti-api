package mx.uam.sapcyti.academic.domain.port.out;

import java.util.List;

/**
 * Keeps professor catalog operations independent from the trimestral persistence model.
 */
public interface ProfessorTrimestralAssignmentsPort {

    List<OpenGroupAssignment> findOpenGroupAssignments(
            Long professorId, Long graduateProgramId);

    void refreshOpenPlanSnapshots(
            Long professorId,
            Long graduateProgramId,
            String employeeNumber,
            String fullName);

    record OpenGroupAssignment(
            Long planId,
            String term,
            Long ueaId,
            String clave,
            String grupo) {}
}

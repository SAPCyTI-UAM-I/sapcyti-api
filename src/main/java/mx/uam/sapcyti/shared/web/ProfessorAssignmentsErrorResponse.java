package mx.uam.sapcyti.shared.web;

import java.util.List;
import mx.uam.sapcyti.academic.domain.exception.ProfessorHasActiveAssignmentsException;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorTrimestralAssignmentsPort.OpenGroupAssignment;

/**
 * Structured conflict returned when deactivating a professor that still has assignments.
 */
public record ProfessorAssignmentsErrorResponse(
        String error,
        String message,
        boolean hasTutorOrAdvisorAssignments,
        List<OpenGroupAssignment> openGroupAssignments) {

    public static ProfessorAssignmentsErrorResponse from(
            ProfessorHasActiveAssignmentsException exception) {
        return new ProfessorAssignmentsErrorResponse(
                ProfessorHasActiveAssignmentsException.ERROR_CODE,
                ProfessorHasActiveAssignmentsException.MESSAGE,
                exception.hasTutorOrAdvisorAssignments(),
                exception.getOpenGroupAssignments());
    }
}

package mx.uam.sapcyti.academic.domain.exception;

import java.util.List;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorTrimestralAssignmentsPort.OpenGroupAssignment;

/**
 * Thrown when a professor is still required by an active academic or open planning assignment.
 */
public class ProfessorHasActiveAssignmentsException extends RuntimeException {

    public static final String ERROR_CODE = "PROFESSOR_HAS_ACTIVE_ASSIGNMENTS";
    public static final String MESSAGE =
            "Professor has active tutor/advisor or open trimestral group assignments";

    private final boolean hasTutorOrAdvisorAssignments;
    private final List<OpenGroupAssignment> openGroupAssignments;

    public ProfessorHasActiveAssignmentsException(
            boolean hasTutorOrAdvisorAssignments,
            List<OpenGroupAssignment> openGroupAssignments) {
        super(MESSAGE);
        this.hasTutorOrAdvisorAssignments = hasTutorOrAdvisorAssignments;
        this.openGroupAssignments = List.copyOf(openGroupAssignments);
    }

    public boolean hasTutorOrAdvisorAssignments() {
        return hasTutorOrAdvisorAssignments;
    }

    public List<OpenGroupAssignment> getOpenGroupAssignments() {
        return openGroupAssignments;
    }
}

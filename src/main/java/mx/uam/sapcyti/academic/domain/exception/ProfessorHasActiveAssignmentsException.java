package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when deactivating a professor assigned as tutor/advisor of an active program (HU-45).
 */
public class ProfessorHasActiveAssignmentsException extends RuntimeException {

    public static final String MESSAGE =
            "Professor is tutor or advisor of an active student program";

    public ProfessorHasActiveAssignmentsException() {
        super(MESSAGE);
    }
}

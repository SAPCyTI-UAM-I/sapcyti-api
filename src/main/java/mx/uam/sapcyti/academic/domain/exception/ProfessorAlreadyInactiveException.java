package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when deactivating a professor who is already inactive (HU-45).
 */
public class ProfessorAlreadyInactiveException extends RuntimeException {

    public static final String MESSAGE = "Professor is already inactive";

    public ProfessorAlreadyInactiveException() {
        super(MESSAGE);
    }
}

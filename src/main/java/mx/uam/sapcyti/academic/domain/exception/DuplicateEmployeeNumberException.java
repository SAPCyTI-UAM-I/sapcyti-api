package mx.uam.sapcyti.academic.domain.exception;

/**
 * Thrown when registering a professor with a duplicate employee number (HU-21).
 */
public class DuplicateEmployeeNumberException extends RuntimeException {

    public static final String ERROR_CODE = "DUPLICATE_EMPLOYEE_NUMBER";
    public static final String MESSAGE = "A professor with this employee number already exists";

    public DuplicateEmployeeNumberException() {
        super(MESSAGE);
    }
}

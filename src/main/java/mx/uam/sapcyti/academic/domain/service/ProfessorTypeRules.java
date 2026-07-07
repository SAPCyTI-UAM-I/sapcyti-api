package mx.uam.sapcyti.academic.domain.service;

import mx.uam.sapcyti.academic.domain.exception.EmployeeNumberImmutableException;
import mx.uam.sapcyti.academic.domain.exception.InvalidTypeChangeException;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;

/**
 * Shared validation for professor type and employee number rules (HU-21, HU-24).
 */
public final class ProfessorTypeRules {

    public static final String EMPLOYEE_NUMBER_REQUIRED =
            "Employee number is required for internal professors";
    public static final String EMPLOYEE_NUMBER_NOT_ALLOWED =
            "Employee number must not be provided for external professors";

    private ProfessorTypeRules() {
    }

    /**
     * Validates type and NEMP changes on update (HU-24 correction, SPEC-031).
     * NEMP is immutable once assigned; type change is one-way (EXTERNO → INTERNO only).
     */
    public static void assertUpdateAllowed(
            ProfessorType currentType,
            String currentEmployeeNumber,
            ProfessorType requestedType,
            String requestedEmployeeNumber) {
        if (currentType == ProfessorType.INTERNO && requestedType == ProfessorType.EXTERNO) {
            throw new InvalidTypeChangeException();
        }
        if (currentEmployeeNumber != null) {
            String trimmedRequested = requestedEmployeeNumber == null || requestedEmployeeNumber.isBlank()
                    ? null
                    : requestedEmployeeNumber.trim();
            if (trimmedRequested == null || !currentEmployeeNumber.equals(trimmedRequested)) {
                throw new EmployeeNumberImmutableException();
            }
        }
    }

    public static String normalizeEmployeeNumber(ProfessorType professorType, String employeeNumber) {
        if (professorType == ProfessorType.EXTERNO) {
            if (employeeNumber != null && !employeeNumber.isBlank()) {
                throw new IllegalArgumentException(EMPLOYEE_NUMBER_NOT_ALLOWED);
            }
            return null;
        }
        if (employeeNumber == null || employeeNumber.isBlank()) {
            throw new IllegalArgumentException(EMPLOYEE_NUMBER_REQUIRED);
        }
        return employeeNumber.trim();
    }
}

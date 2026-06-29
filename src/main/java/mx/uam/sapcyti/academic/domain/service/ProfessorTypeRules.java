package mx.uam.sapcyti.academic.domain.service;

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

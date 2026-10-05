package mx.uam.sapcyti.trimestral.application.service;

import java.util.Comparator;
import mx.uam.sapcyti.academic.domain.model.Student;

/**
 * Canonical global priority for trimestral demand and manual group ordering.
 */
final class StudentPriority {

    private static final Comparator<Student> COMPARATOR = Comparator.comparing(
                    (Student student) -> student.getPersonalData().getFirstLastName(),
                    Comparator.nullsLast(String::compareToIgnoreCase))
            .thenComparing(
                    student -> student.getPersonalData().getSecondLastName(),
                    Comparator.nullsLast(String::compareToIgnoreCase))
            .thenComparing(
                    student -> student.getPersonalData().getFirstName(),
                    Comparator.nullsLast(String::compareToIgnoreCase))
            .thenComparing(
                    Student::getEnrollmentId,
                    Comparator.nullsLast(String::compareTo));

    private StudentPriority() {
    }

    static int compare(Student left, Student right) {
        if (left == null || right == null) {
            return 0;
        }
        return COMPARATOR.compare(left, right);
    }
}

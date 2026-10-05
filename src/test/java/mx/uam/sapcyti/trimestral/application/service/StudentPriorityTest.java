package mx.uam.sapcyti.trimestral.application.service;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Student;
import org.junit.jupiter.api.Test;

class StudentPriorityTest {

    @Test
    void enrollmentBreaksEqualNameTies() {
        Student higherEnrollment = student("002");
        Student lowerEnrollment = student("001");
        List<Student> ordered = new ArrayList<>(List.of(higherEnrollment, lowerEnrollment));

        ordered.sort(StudentPriority::compare);

        assertThat(ordered)
                .extracting(Student::getEnrollmentId)
                .containsExactly("001", "002");
    }

    private static Student student(String enrollmentId) {
        return new Student(
                enrollmentId,
                1L,
                1L,
                null,
                new PersonalData(
                        "Ana",
                        "Lopez",
                        "Diaz",
                        "Mexicana",
                        null,
                        "5550000000",
                        null),
                sampleAcademicInformation());
    }
}

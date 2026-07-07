package mx.uam.sapcyti.academic;

import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.AcademicInformation;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorInformation;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

public final class AcademicTestFixtures {

    private AcademicTestFixtures() {
    }

    public static PersonalData studentPersonalData() {
        return new PersonalData(
                "Paulina",
                "Valencia",
                "Franco",
                "Mexicana",
                LocalDate.of(1998, 3, 15),
                "5554821234",
                "1234");
    }

    public static PersonalData studentPersonalDataWithoutExtension() {
        return new PersonalData(
                "Paulina",
                "Valencia",
                "Franco",
                "Mexicana",
                LocalDate.of(1998, 3, 15),
                "5554821234",
                null);
    }

    public static AcademicInformation sampleAcademicInformation() {
        return new AcademicInformation(
                "Computación",
                DegreeLevel.LICENCIATURA,
                ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1));
    }

    public static PersonalData professorPersonalData() {
        return new PersonalData("Humberto", "Cervantes", "Maceda", null, null, "5554825678", null);
    }

    public static PersonalData minimalProfessorPersonalData(String firstName, String firstLastName) {
        return new PersonalData(firstName, firstLastName, null, null, null, "5554820000", null);
    }

    public static ProfessorInformation defaultProfessorInformation() {
        return new ProfessorInformation(false, null, null);
    }

    public static Professor internoProfessor(
            String employeeNumber, Long userId, Long graduateProgramId, PersonalData personalData) {
        return new Professor(
                ProfessorType.INTERNO,
                employeeNumber,
                userId,
                graduateProgramId,
                personalData,
                defaultProfessorInformation());
    }

    public static Professor internoProfessor(
            String employeeNumber,
            Long userId,
            Long graduateProgramId,
            PersonalData personalData,
            ProfessorInformation information) {
        return new Professor(
                ProfessorType.INTERNO, employeeNumber, userId, graduateProgramId, personalData, information);
    }
}

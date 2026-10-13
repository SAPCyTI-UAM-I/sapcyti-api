package mx.uam.sapcyti.academic.domain.model;

/**
 * The one {@link StudentProgram} of the authorized graduate program whose type equals
 * {@link AcademicInformation#getProgramType()}. Degree and status come from that row.
 * {@link DegreeLevel} is not a source. {@code secondLastName} is null when the maternal surname is absent.
 */
public record StudentExamContext(
        Long studentId,
        Long graduateProgramId,
        Long studentProgramId,
        ProgramType programType,
        ProgramStatus programStatus,
        String firstName,
        String firstLastName,
        String secondLastName) {
}

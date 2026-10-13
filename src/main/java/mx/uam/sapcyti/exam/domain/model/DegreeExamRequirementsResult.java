package mx.uam.sapcyti.exam.domain.model;

/**
 * GRADO requirements, or {@code ACADEMIC_PROGRAM_CHANGED} when the stored binding
 * does not match the current program or degree. A conflict does not open another request.
 */
public record DegreeExamRequirementsResult(ExamRequirements requirements, String error) {

    public static final String ACADEMIC_PROGRAM_CHANGED = "ACADEMIC_PROGRAM_CHANGED";

    public DegreeExamRequirementsResult {
        boolean conflict = ACADEMIC_PROGRAM_CHANGED.equals(error);
        if (error != null && !conflict) {
            throw new IllegalArgumentException("unsupported requirements error");
        }
        if (conflict == (requirements != null)) {
            throw new IllegalArgumentException("requirements result is inconsistent");
        }
    }

    public static DegreeExamRequirementsResult ready(ExamRequirements requirements) {
        return new DegreeExamRequirementsResult(requirements, null);
    }

    public static DegreeExamRequirementsResult academicProgramChanged() {
        return new DegreeExamRequirementsResult(null, ACADEMIC_PROGRAM_CHANGED);
    }
}

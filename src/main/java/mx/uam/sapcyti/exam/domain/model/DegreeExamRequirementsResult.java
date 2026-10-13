package mx.uam.sapcyti.exam.domain.model;

/**
 * Requirements for one exam type, or a refusal that does not open another request.
 * {@code ACADEMIC_PROGRAM_CHANGED} means the stored binding does not match the current program
 * or degree. {@code EXAM_TYPE_NOT_ALLOWED} means {@code PREDOCTORAL} was asked for a degree
 * other than doctorate.
 */
public record DegreeExamRequirementsResult(ExamRequirements requirements, String error) {

    public static final String ACADEMIC_PROGRAM_CHANGED = "ACADEMIC_PROGRAM_CHANGED";
    public static final String EXAM_TYPE_NOT_ALLOWED = "EXAM_TYPE_NOT_ALLOWED";

    public DegreeExamRequirementsResult {
        boolean rejected = ACADEMIC_PROGRAM_CHANGED.equals(error) || EXAM_TYPE_NOT_ALLOWED.equals(error);
        if (error != null && !rejected) {
            throw new IllegalArgumentException("unsupported requirements error");
        }
        if (rejected == (requirements != null)) {
            throw new IllegalArgumentException("requirements result is inconsistent");
        }
    }

    public static DegreeExamRequirementsResult ready(ExamRequirements requirements) {
        return new DegreeExamRequirementsResult(requirements, null);
    }

    public static DegreeExamRequirementsResult academicProgramChanged() {
        return new DegreeExamRequirementsResult(null, ACADEMIC_PROGRAM_CHANGED);
    }

    public static DegreeExamRequirementsResult examTypeNotAllowed() {
        return new DegreeExamRequirementsResult(null, EXAM_TYPE_NOT_ALLOWED);
    }
}

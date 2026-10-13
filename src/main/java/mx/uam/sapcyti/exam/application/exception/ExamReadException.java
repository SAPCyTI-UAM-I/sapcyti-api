package mx.uam.sapcyti.exam.application.exception;

import java.util.List;

/**
 * Failure of the two HU-62 reads. Only {@link Code#VALIDATION_ERROR} carries field errors.
 * Every other code is published without {@code details}.
 */
public class ExamReadException extends RuntimeException {

    /**
     * Wire codes of these reads. {@code PROGRAM_NOT_ELIGIBLE} is a later write and is not a member.
     */
    public enum Code {
        FORBIDDEN,
        EXAM_TYPE_NOT_ALLOWED,
        STUDENT_NOT_FOUND,
        VALIDATION_ERROR,
        ACADEMIC_PROGRAM_AMBIGUOUS,
        ACADEMIC_PROGRAM_CHANGED,
        STUDENT_DATA_INCOMPLETE,
        DEGREE_UNSUPPORTED,
        PROGRAM_STATUS_UNSUPPORTED,
        DEPENDENCY_UNAVAILABLE
    }

    private final Code code;
    private final List<ExamRequestFieldError> fieldErrors;

    private ExamReadException(Code code, String message, List<ExamRequestFieldError> fieldErrors) {
        super(message);
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public Code code() {
        return code;
    }

    public List<ExamRequestFieldError> fieldErrors() {
        return fieldErrors;
    }

    public static ExamReadException forbidden() {
        return withoutDetails(Code.FORBIDDEN, "Access denied");
    }

    public static ExamReadException examTypeNotAllowed() {
        return withoutDetails(Code.EXAM_TYPE_NOT_ALLOWED, "Predoctoral is available only for doctoral students");
    }

    public static ExamReadException studentNotFound() {
        return withoutDetails(Code.STUDENT_NOT_FOUND, "Student program was not found");
    }

    public static ExamReadException academicProgramAmbiguous() {
        return withoutDetails(Code.ACADEMIC_PROGRAM_AMBIGUOUS, "Academic program is ambiguous");
    }

    public static ExamReadException academicProgramChanged() {
        return withoutDetails(Code.ACADEMIC_PROGRAM_CHANGED, "Academic program changed");
    }

    public static ExamReadException studentDataIncomplete() {
        return withoutDetails(Code.STUDENT_DATA_INCOMPLETE, "Student data is incomplete");
    }

    public static ExamReadException degreeUnsupported() {
        return withoutDetails(Code.DEGREE_UNSUPPORTED, "Degree is not supported");
    }

    public static ExamReadException programStatusUnsupported() {
        return withoutDetails(Code.PROGRAM_STATUS_UNSUPPORTED, "Program status is not supported");
    }

    public static ExamReadException dependencyUnavailable() {
        return withoutDetails(Code.DEPENDENCY_UNAVAILABLE, "Exam requirements are temporarily unavailable");
    }

    public static ExamReadException validation(String message, List<ExamRequestFieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            throw new IllegalArgumentException("VALIDATION_ERROR requires field errors");
        }
        return new ExamReadException(Code.VALIDATION_ERROR, message, fieldErrors);
    }

    private static ExamReadException withoutDetails(Code code, String message) {
        return new ExamReadException(code, message, List.of());
    }
}

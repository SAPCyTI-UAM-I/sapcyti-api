package mx.uam.sapcyti.exam.domain.exception;

/**
 * A second row for the same {@code (studentId, examType)} was rejected by the unique index.
 * The existing row stays. This is not a second request.
 */
public class ExamDraftConflictException extends RuntimeException {

    public ExamDraftConflictException() {
        super("Exam request already exists for this student and exam type");
    }
}

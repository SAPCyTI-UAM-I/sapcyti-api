package mx.uam.sapcyti.academic.domain.port.out;

import mx.uam.sapcyti.academic.domain.model.StudentExamContextResult;

/**
 * Reads the academic row Exam Requests may use for the authenticated student.
 * {@code graduateProgramId} is already authorized by the caller.
 */
public interface StudentExamContextPort {

    StudentExamContextResult resolve(Long userId, Long graduateProgramId);
}

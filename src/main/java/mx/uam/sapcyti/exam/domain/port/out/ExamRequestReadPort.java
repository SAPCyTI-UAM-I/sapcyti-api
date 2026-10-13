package mx.uam.sapcyti.exam.domain.port.out;

import java.util.Optional;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamType;

/**
 * Reads the dossier already stored for one student and exam type.
 * A miss is empty and does not insert a row. The result has no object key, bucket or hash.
 */
public interface ExamRequestReadPort {

    Optional<ExamRequestRead> findByStudentAndType(Long studentId, ExamType examType);
}

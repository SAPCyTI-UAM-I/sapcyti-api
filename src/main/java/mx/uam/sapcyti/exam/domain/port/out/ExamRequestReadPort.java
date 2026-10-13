package mx.uam.sapcyti.exam.domain.port.out;

import java.util.Optional;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamType;

/**
 * Reads the dossier already stored for one student and exam type.
 * HU-62 does not insert a row. The production adapter stays empty until HU-65 replaces it.
 */
public interface ExamRequestReadPort {

    Optional<ExamRequestRead> findByStudentAndType(Long studentId, ExamType examType);
}

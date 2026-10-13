package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import java.util.Optional;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import org.springframework.stereotype.Component;

/**
 * Production {@link ExamRequestReadPort} for HU-62. There is no dossier table yet, so every read is empty.
 * HU-65 replaces this class. Do not register a second implementation beside it.
 */
@Component
public class EmptyExamRequestReadAdapter implements ExamRequestReadPort {

    @Override
    public Optional<ExamRequestRead> findByStudentAndType(Long studentId, ExamType examType) {
        return Optional.empty();
    }
}

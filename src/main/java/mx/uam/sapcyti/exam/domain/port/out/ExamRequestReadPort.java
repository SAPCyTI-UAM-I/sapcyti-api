package mx.uam.sapcyti.exam.domain.port.out;

import java.util.Optional;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.StoredDocumentReference;

/**
 * Reads the dossier already stored for one student and exam type.
 * A miss is empty and does not insert a row.
 * {@link #findByStudentAndType} has no object key, bucket or hash.
 */
public interface ExamRequestReadPort {

    Optional<ExamRequestRead> findByStudentAndType(Long studentId, ExamType examType);

    /**
     * Current reference of one type, including the persisted object key.
     * Empty when the request or that current type is missing. Does not insert a row.
     * This is not a panel DTO.
     */
    Optional<StoredDocumentReference> findCurrentReference(
            Long studentId, ExamType examType, DocumentType documentType);
}

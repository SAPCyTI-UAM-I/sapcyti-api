package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataExamDocumentRepository extends JpaRepository<ExamDocumentEntity, Long> {

    List<ExamDocumentEntity> findByExamRequestIdOrderByIdAsc(Long examRequestId);

    Optional<ExamDocumentEntity> findByExamRequestIdAndDocumentType(Long examRequestId, DocumentType documentType);
}

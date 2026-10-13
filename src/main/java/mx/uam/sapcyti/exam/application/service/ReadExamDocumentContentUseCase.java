package mx.uam.sapcyti.exam.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.exam.application.exception.ExamContentException;
import mx.uam.sapcyti.exam.application.model.ExamContentPayload;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.OpenedObject;
import mx.uam.sapcyti.exam.domain.model.StoredDocumentReference;
import mx.uam.sapcyti.exam.domain.port.out.DocumentStoragePort;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the current PDF of the caller's own exam request (SPEC-053).
 *
 * <p>Identity comes from the JWT. This read does not call {@code StudentExamContextPort},
 * does not recalculate eligibility and does not change the request. A missing reference and
 * a reference outside the persisted program scope are the same not-found result. {@code kind}
 * URL is not opened.
 */
@Service
public class ReadExamDocumentContentUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ReadExamDocumentContentUseCase.class);

    private final StudentRepositoryPort studentRepository;
    private final ExamRequestReadPort examRequestReadPort;
    private final DocumentStoragePort documentStoragePort;

    public ReadExamDocumentContentUseCase(
            StudentRepositoryPort studentRepository,
            ExamRequestReadPort examRequestReadPort,
            DocumentStoragePort documentStoragePort) {
        this.studentRepository = studentRepository;
        this.examRequestReadPort = examRequestReadPort;
        this.documentStoragePort = documentStoragePort;
    }

    /**
     * Opens the current PDF before the caller writes the 200. {@code disposition} is {@code inline}
     * or {@code attachment}. The object key is the one stored on the current reference.
     */
    @Transactional(readOnly = true)
    public ExamContentPayload read(
            Long userId,
            RoleType role,
            Long graduateProgramId,
            ExamType examType,
            DocumentType documentType,
            String disposition) {
        if (role != RoleType.STUDENT || graduateProgramId == null) {
            throw ExamContentException.forbidden();
        }
        Long studentId = studentId(userId, graduateProgramId);
        Optional<ExamRequestRead> dossier = dossier(studentId, examType);
        if (dossier.isEmpty() || !graduateProgramId.equals(dossier.get().binding().graduateProgramId())) {
            throw ExamContentException.notFound();
        }
        CurrentExamDocument current = current(dossier.get(), documentType);
        if (current == null) {
            throw ExamContentException.notFound();
        }
        if (current.kind() == DocumentKind.URL) {
            throw ExamContentException.externalUrl();
        }
        StoredDocumentReference reference = reference(studentId, examType, documentType);
        if (reference == null || !current.id().equals(reference.id()) || reference.kind() != DocumentKind.PDF) {
            throw ExamContentException.notFound();
        }
        OpenedObject opened = open(reference.objectKey());
        if (opened.sizeBytes() != reference.sizeBytes()) {
            close(opened.body());
            throw ExamContentException.storageUnavailable();
        }
        return new ExamContentPayload(disposition, reference.originalName(), opened);
    }

    private Long studentId(Long userId, Long graduateProgramId) {
        try {
            Optional<Student> student = studentRepository.findByUserIdAndGraduateProgramId(userId, graduateProgramId);
            if (student.isEmpty() || student.get().getId() == null) {
                throw ExamContentException.notFound();
            }
            return student.get().getId();
        } catch (ExamContentException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            LOG.warn("Exam document reference could not be read");
            throw ExamContentException.storageUnavailable();
        }
    }

    private Optional<ExamRequestRead> dossier(Long studentId, ExamType examType) {
        try {
            return examRequestReadPort.findByStudentAndType(studentId, examType);
        } catch (DataAccessException ex) {
            LOG.warn("Exam document reference could not be read");
            throw ExamContentException.storageUnavailable();
        }
    }

    private StoredDocumentReference reference(Long studentId, ExamType examType, DocumentType documentType) {
        try {
            return examRequestReadPort.findCurrentReference(studentId, examType, documentType).orElse(null);
        } catch (DataAccessException ex) {
            LOG.warn("Exam document reference could not be read");
            throw ExamContentException.storageUnavailable();
        }
    }

    private OpenedObject open(String objectKey) {
        try {
            OpenedObject opened = documentStoragePort.open(objectKey);
            if (opened == null || opened.body() == null) {
                throw ExamContentException.storageUnavailable();
            }
            return opened;
        } catch (ExamContentException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOG.warn("Document content could not be opened");
            throw ExamContentException.storageUnavailable();
        }
    }

    private static CurrentExamDocument current(ExamRequestRead dossier, DocumentType documentType) {
        for (CurrentExamDocument document : dossier.documents()) {
            if (document.documentType() == documentType) {
                return document;
            }
        }
        return null;
    }

    private static void close(InputStream body) {
        try {
            body.close();
        } catch (IOException ex) {
            LOG.warn("Document content could not be opened");
        }
    }
}

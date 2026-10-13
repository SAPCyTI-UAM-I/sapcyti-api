package mx.uam.sapcyti.exam.application.service;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.academic.domain.model.StudentExamContextResult;
import mx.uam.sapcyti.academic.domain.port.out.StudentExamContextPort;
import mx.uam.sapcyti.exam.application.exception.ExamDraftException;
import mx.uam.sapcyti.exam.application.exception.ExamDraftFieldError;
import mx.uam.sapcyti.exam.application.exception.ExamReadException;
import mx.uam.sapcyti.exam.application.model.DraftCommands;
import mx.uam.sapcyti.exam.application.model.DraftOutcomes;
import mx.uam.sapcyti.exam.domain.exception.ExamDocumentNotFoundException;
import mx.uam.sapcyti.exam.domain.exception.ExamDraftConflictException;
import mx.uam.sapcyti.exam.domain.exception.ExamDraftNotFoundException;
import mx.uam.sapcyti.exam.domain.exception.ExamRequestNotDraftException;
import mx.uam.sapcyti.exam.domain.exception.ObjectStorageException;
import mx.uam.sapcyti.exam.domain.exception.StaleExamDraftException;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.DocumentValidationError;
import mx.uam.sapcyti.exam.domain.model.DocumentValidationVerdict;
import mx.uam.sapcyti.exam.domain.model.ExamObjectKey;
import mx.uam.sapcyti.exam.domain.model.ExamRequestBinding;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamRequestStatus;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.PrivateObjectPut;
import mx.uam.sapcyti.exam.domain.model.RequirementStatus;
import mx.uam.sapcyti.exam.domain.model.StoredDocumentReference;
import mx.uam.sapcyti.exam.domain.model.StoredObjectReceipt;
import mx.uam.sapcyti.exam.domain.port.out.DocumentStoragePort;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestDraftPort;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import mx.uam.sapcyti.exam.domain.service.DegreeExamCatalog;
import mx.uam.sapcyti.exam.domain.service.ExamDocumentValidator;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Saves a draft batch, replaces or deletes one document, and reads the panel (SPEC-052).
 *
 * <p>Identity comes from the JWT. The body is not a source of {@code studentId} or degree.
 * Each document is judged by {@link ExamDocumentValidator}. An accepted PDF is written through
 * {@link DocumentStoragePort} before {@link ExamRequestDraftPort} commits. The version lock is
 * taken once for the whole batch. Saving does not submit the request.
 */
@Service
public class ExamDraftUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ExamDraftUseCase.class);
    private static final String STORAGE_WRITE_FAILED = "STORAGE_WRITE_FAILED";
    private static final String STORAGE_FAILED = "The document could not be stored";

    private final StudentExamContextPort studentExamContextPort;
    private final ExamRequestReadPort examRequestReadPort;
    private final ExamRequestDraftPort examRequestDraftPort;
    private final DocumentStoragePort documentStoragePort;
    private final ExamDocumentValidator examDocumentValidator;
    private final DegreeExamCatalog degreeExamCatalog;

    public ExamDraftUseCase(
            StudentExamContextPort studentExamContextPort,
            ExamRequestReadPort examRequestReadPort,
            ExamRequestDraftPort examRequestDraftPort,
            DocumentStoragePort documentStoragePort,
            ExamDocumentValidator examDocumentValidator,
            DegreeExamCatalog degreeExamCatalog) {
        this.studentExamContextPort = studentExamContextPort;
        this.examRequestReadPort = examRequestReadPort;
        this.examRequestDraftPort = examRequestDraftPort;
        this.documentStoragePort = documentStoragePort;
        this.examDocumentValidator = examDocumentValidator;
        this.degreeExamCatalog = degreeExamCatalog;
    }

    /**
     * Saves the accepted descriptors. HTTP stays a successful batch even when every descriptor fails.
     * The version increases once, and only when at least one descriptor is stored.
     */
    public DraftOutcomes.Save save(
            Long userId, RoleType role, Long graduateProgramId, ExamType examType, DraftCommands.Batch batch) {
        StudentExamContext context = resolve(userId, role, graduateProgramId);
        Degree degree = degreeOf(context);
        rejectIneligibleWrite(context, examType, degree);
        Optional<ExamRequestRead> current = read(context.studentId(), examType);
        rejectBinding(context, degree, current);
        requireBatchVersion(current, batch.version());

        List<DraftOutcomes.Item> results = new ArrayList<>();
        List<Accepted> accepted = new ArrayList<>();
        for (DraftCommands.Document document : batch.documents()) {
            DocumentValidationVerdict verdict = examDocumentValidator.validate(
                    context,
                    examType,
                    document.documentType(),
                    document.kind(),
                    document.originalName(),
                    document.pdfBytes(),
                    document.externalUrl());
            if (!verdict.accepted()) {
                results.add(errorItem(document.documentType(), verdict.error()));
            } else {
                accepted.add(new Accepted(results.size(), document));
                results.add(pending(document.documentType()));
            }
        }
        if (accepted.isEmpty()) {
            return unchanged(context, examType, current, results);
        }

        long requestId = current.map(read -> read.binding().id()).orElseGet(examRequestDraftPort::reserveRequestId);
        Instant uploadedAt = Instant.now();
        List<StoredDocumentReference> stored = new ArrayList<>();
        List<StoredObjectReceipt> written = new ArrayList<>();
        for (Accepted item : accepted) {
            try {
                stored.add(storeOne(
                        context, requestId, item.document(), uploadedAt, written));
            } catch (ObjectStorageException ex) {
                results.set(item.index(), storageItem(item.document().documentType()));
            }
        }
        if (stored.isEmpty()) {
            return unchanged(context, examType, current, results);
        }
        ExamRequestRead saved = commitBatch(current, context, examType, degree, requestId, batch.version(), stored, written);
        fillSaved(results, saved);
        return new DraftOutcomes.Save(reload(context, examType), saved.binding().version(), results);
    }

    /**
     * Replaces one PDF on an existing draft. Does not create a draft.
     */
    public DraftOutcomes.Mutation replacePdf(
            Long userId,
            RoleType role,
            Long graduateProgramId,
            ExamType examType,
            DocumentType documentType,
            DraftCommands.PdfReplacement command) {
        PreparedMutation prepared = prepareMutation(
                userId, role, graduateProgramId, examType, documentType, command.version());
        rejectVerdict(examDocumentValidator.validate(
                prepared.context(),
                examType,
                documentType,
                DocumentKind.PDF,
                command.originalName(),
                command.pdfBytes(),
                null));
        Instant uploadedAt = Instant.now();
        List<StoredObjectReceipt> written = new ArrayList<>();
        StoredDocumentReference reference = storePdf(
                prepared.context(),
                prepared.current().binding().id(),
                documentType,
                command.originalName(),
                command.pdfBytes(),
                uploadedAt,
                written);
        ExamRequestRead saved = commitSave(prepared, List.of(reference), written);
        CurrentExamDocument document = currentOf(saved, documentType);
        return new DraftOutcomes.Mutation(reload(prepared.context(), examType), document);
    }

    /**
     * Replaces one article URL. No object is written and the URL is not fetched.
     */
    public DraftOutcomes.Mutation replaceUrl(
            Long userId,
            RoleType role,
            Long graduateProgramId,
            ExamType examType,
            DocumentType documentType,
            DraftCommands.UrlReplacement command) {
        PreparedMutation prepared = prepareMutation(
                userId, role, graduateProgramId, examType, documentType, command.version());
        rejectVerdict(examDocumentValidator.validate(
                prepared.context(),
                examType,
                documentType,
                DocumentKind.URL,
                null,
                null,
                command.externalUrl()));
        long documentId = examRequestDraftPort.reserveDocumentId();
        StoredDocumentReference reference = urlReference(
                prepared.current().binding().id(),
                documentId,
                documentType,
                command.externalUrl(),
                Instant.now());
        ExamRequestRead saved = commitSave(prepared, List.of(reference), List.of());
        return new DraftOutcomes.Mutation(reload(prepared.context(), examType), currentOf(saved, documentType));
    }

    /**
     * Removes one current document. Does not create a draft.
     */
    public DraftOutcomes.Mutation delete(
            Long userId,
            RoleType role,
            Long graduateProgramId,
            ExamType examType,
            DocumentType documentType,
            int version) {
        PreparedMutation prepared = prepareMutation(
                userId, role, graduateProgramId, examType, documentType, version);
        try {
            examRequestDraftPort.delete(new ExamRequestDraftPort.Deletion(
                    prepared.context().studentId(),
                    examType,
                    version,
                    documentType,
                    Instant.now()));
        } catch (ExamDraftNotFoundException ex) {
            throw ExamDraftException.requestNotFound();
        } catch (ExamDocumentNotFoundException ex) {
            throw ExamDraftException.documentNotFound();
        } catch (ExamRequestNotDraftException ex) {
            throw ExamDraftException.requestNotDraft();
        } catch (StaleExamDraftException ex) {
            throw ExamDraftException.versionConflict();
        } catch (DataAccessException ex) {
            LOG.warn("Exam draft delete could not be committed");
            throw ExamDraftException.dependencyUnavailable();
        }
        return new DraftOutcomes.Mutation(reload(prepared.context(), examType), null);
    }

    /**
     * Reads the panel. Does not insert a row. {@code BAJA} and {@code SUSPENSION} stay eligible false.
     */
    public DraftOutcomes.Panel panel(Long userId, RoleType role, Long graduateProgramId, ExamType examType) {
        StudentExamContext context = resolve(userId, role, graduateProgramId);
        ExamRequirements state = reload(context, examType);
        int pending = (int) state.requirements().stream()
                .filter(requirement -> requirement.required() && requirement.status() == RequirementStatus.PENDIENTE)
                .count();
        return new DraftOutcomes.Panel(state, pending);
    }

    private DraftOutcomes.Save unchanged(
            StudentExamContext context,
            ExamType examType,
            Optional<ExamRequestRead> current,
            List<DraftOutcomes.Item> results) {
        if (current.isEmpty()) {
            return new DraftOutcomes.Save(null, 0, results);
        }
        ExamRequirements state = reload(context, examType);
        return new DraftOutcomes.Save(state, state.request().version(), results);
    }

    private ExamRequestRead commitBatch(
            Optional<ExamRequestRead> current,
            StudentExamContext context,
            ExamType examType,
            Degree degree,
            long requestId,
            int expectedVersion,
            List<StoredDocumentReference> stored,
            List<StoredObjectReceipt> written) {
        try {
            if (current.isEmpty()) {
                return examRequestDraftPort.create(new ExamRequestDraftPort.Creation(
                        requestId,
                        context.studentId(),
                        examType,
                        context.graduateProgramId(),
                        degree,
                        stored));
            }
            return examRequestDraftPort.save(new ExamRequestDraftPort.Save(
                    context.studentId(), examType, expectedVersion, stored));
        } catch (ExamDraftConflictException | StaleExamDraftException ex) {
            notice(written);
            throw ExamDraftException.versionConflict();
        } catch (ExamRequestNotDraftException ex) {
            notice(written);
            throw ExamDraftException.requestNotDraft();
        } catch (DataAccessException ex) {
            LOG.warn("Exam draft batch could not be committed");
            notice(written);
            throw ExamDraftException.dependencyUnavailable();
        }
    }

    private ExamRequestRead commitSave(
            PreparedMutation prepared, List<StoredDocumentReference> stored, List<StoredObjectReceipt> written) {
        try {
            return examRequestDraftPort.save(new ExamRequestDraftPort.Save(
                    prepared.context().studentId(),
                    prepared.examType(),
                    prepared.version(),
                    stored));
        } catch (ExamDraftConflictException | StaleExamDraftException ex) {
            notice(written);
            throw ExamDraftException.versionConflict();
        } catch (ExamRequestNotDraftException ex) {
            notice(written);
            throw ExamDraftException.requestNotDraft();
        } catch (ExamDraftNotFoundException ex) {
            notice(written);
            throw ExamDraftException.requestNotFound();
        } catch (DataAccessException ex) {
            LOG.warn("Exam draft replacement could not be committed");
            notice(written);
            throw ExamDraftException.dependencyUnavailable();
        }
    }

    private StoredDocumentReference storeOne(
            StudentExamContext context,
            long requestId,
            DraftCommands.Document document,
            Instant uploadedAt,
            List<StoredObjectReceipt> written) {
        long documentId = examRequestDraftPort.reserveDocumentId();
        if (document.kind() == DocumentKind.URL) {
            return urlReference(requestId, documentId, document.documentType(), document.externalUrl(), uploadedAt);
        }
        return storePdf(
                context,
                requestId,
                document.documentType(),
                document.originalName(),
                document.pdfBytes(),
                uploadedAt,
                written,
                documentId);
    }

    private StoredDocumentReference storePdf(
            StudentExamContext context,
            long requestId,
            DocumentType documentType,
            String originalName,
            byte[] pdfBytes,
            Instant uploadedAt,
            List<StoredObjectReceipt> written) {
        return storePdf(
                context,
                requestId,
                documentType,
                originalName,
                pdfBytes,
                uploadedAt,
                written,
                examRequestDraftPort.reserveDocumentId());
    }

    private StoredDocumentReference storePdf(
            StudentExamContext context,
            long requestId,
            DocumentType documentType,
            String originalName,
            byte[] pdfBytes,
            Instant uploadedAt,
            List<StoredObjectReceipt> written,
            long documentId) {
        String objectKey = ExamObjectKey.pdf(
                context.graduateProgramId(), context.studentId(), requestId, documentId);
        String sha256 = sha256(pdfBytes);
        PrivateObjectPut put = new PrivateObjectPut(
                objectKey, sha256, StoredDocumentReference.PDF_MEDIA_TYPE, pdfBytes.length);
        StoredObjectReceipt receipt = documentStoragePort.put(put, new ByteArrayInputStream(pdfBytes));
        written.add(receipt);
        return new StoredDocumentReference(
                documentId,
                requestId,
                documentType,
                DocumentKind.PDF,
                receipt.objectKey(),
                receipt.sha256(),
                receipt.contentType(),
                originalName,
                null,
                receipt.sizeBytes(),
                uploadedAt);
    }

    private static StoredDocumentReference urlReference(
            long requestId, long documentId, DocumentType documentType, String externalUrl, Instant uploadedAt) {
        return new StoredDocumentReference(
                documentId,
                requestId,
                documentType,
                DocumentKind.URL,
                null,
                null,
                null,
                null,
                externalUrl,
                null,
                uploadedAt);
    }

    private void notice(List<StoredObjectReceipt> written) {
        for (StoredObjectReceipt receipt : written) {
            documentStoragePort.noticeUncommitted(receipt.objectKey(), receipt.sha256());
        }
    }

    private PreparedMutation prepareMutation(
            Long userId,
            RoleType role,
            Long graduateProgramId,
            ExamType examType,
            DocumentType documentType,
            int version) {
        if (version < 1) {
            throw ExamDraftException.validation(
                    "Invalid version",
                    List.of(new ExamDraftFieldError("version", "OUT_OF_RANGE", "Version must be at least 1")));
        }
        StudentExamContext context = resolve(userId, role, graduateProgramId);
        Degree degree = degreeOf(context);
        rejectIneligibleWrite(context, examType, degree);
        ExamRequestRead current = read(context.studentId(), examType)
                .orElseThrow(ExamDraftException::requestNotFound);
        if (changed(context, degree, current.binding())) {
            throw ExamDraftException.academicProgramChanged();
        }
        if (current.binding().status() != ExamRequestStatus.BORRADOR) {
            throw ExamDraftException.requestNotDraft();
        }
        if (current.binding().version() != version) {
            throw ExamDraftException.versionConflict();
        }
        return new PreparedMutation(context, examType, documentType, version, current);
    }

    private void rejectIneligibleWrite(StudentExamContext context, ExamType examType, Degree degree) {
        if (degreeExamCatalog.writeRejection(context).isPresent()) {
            throw ExamDraftException.programNotEligible();
        }
        if (examType == ExamType.PREDOCTORAL && degree != Degree.DOCTORADO) {
            throw ExamDraftException.examTypeNotAllowed();
        }
    }

    private void rejectBinding(StudentExamContext context, Degree degree, Optional<ExamRequestRead> current) {
        if (current.isPresent() && changed(context, degree, current.get().binding())) {
            throw ExamDraftException.academicProgramChanged();
        }
        if (current.isPresent() && current.get().binding().status() != ExamRequestStatus.BORRADOR) {
            throw ExamDraftException.requestNotDraft();
        }
    }

    private static void requireBatchVersion(Optional<ExamRequestRead> current, int version) {
        int stored = current.map(read -> read.binding().version()).orElse(0);
        if (version != stored) {
            throw ExamDraftException.versionConflict();
        }
    }

    private ExamRequirements reload(StudentExamContext context, ExamType examType) {
        try {
            var result = degreeExamCatalog.requirements(context, examType);
            if (mx.uam.sapcyti.exam.domain.model.DegreeExamRequirementsResult.ACADEMIC_PROGRAM_CHANGED
                    .equals(result.error())) {
                throw ExamDraftException.academicProgramChanged();
            }
            if (mx.uam.sapcyti.exam.domain.model.DegreeExamRequirementsResult.EXAM_TYPE_NOT_ALLOWED
                    .equals(result.error())) {
                throw ExamDraftException.examTypeNotAllowed();
            }
            return result.requirements();
        } catch (DataAccessException ex) {
            LOG.warn("Exam panel could not be read");
            throw ExamDraftException.dependencyUnavailable();
        }
    }

    private Optional<ExamRequestRead> read(Long studentId, ExamType examType) {
        try {
            return examRequestReadPort.findByStudentAndType(studentId, examType);
        } catch (DataAccessException ex) {
            LOG.warn("Exam draft could not be read");
            throw ExamDraftException.dependencyUnavailable();
        }
    }

    private StudentExamContext resolve(Long userId, RoleType role, Long graduateProgramId) {
        if (role != RoleType.STUDENT || graduateProgramId == null) {
            throw ExamDraftException.forbidden();
        }
        StudentExamContextResult result;
        try {
            result = studentExamContextPort.resolve(userId, graduateProgramId);
        } catch (DataAccessException ex) {
            LOG.warn("Student exam context could not be read");
            throw ExamDraftException.dependencyUnavailable();
        }
        return switch (result.outcome()) {
            case RESOLVED -> result.context();
            case NOT_FOUND -> throw ExamDraftException.studentNotFound();
            case AMBIGUOUS -> throw ExamDraftException.academicProgramAmbiguous();
            case INCOMPLETE -> throw ExamDraftException.studentDataIncomplete();
            case DEGREE_UNSUPPORTED -> throw ExamReadException.degreeUnsupported();
            case STATUS_UNSUPPORTED -> throw ExamReadException.programStatusUnsupported();
        };
    }

    private static void rejectVerdict(DocumentValidationVerdict verdict) {
        if (verdict.accepted()) {
            return;
        }
        DocumentValidationError error = verdict.error();
        switch (error.error()) {
            case "UNSAFE_FILE_NAME" -> throw ExamDraftException.unsafeFileName(error.message());
            case "INVALID_DOCUMENT_NAME" -> throw ExamDraftException.invalidDocumentName(
                    error.message(), error.details().expectedName());
            case "INVALID_PDF_CONTENT" -> throw ExamDraftException.invalidPdfContent(error.message());
            case "INVALID_ARTICLE_URL" -> throw ExamDraftException.invalidArticleUrl(
                    error.message(), error.details().fieldErrors());
            case "DOCUMENT_KIND_NOT_ALLOWED" -> throw ExamDraftException.documentKindNotAllowed(error.message());
            case "STUDENT_DATA_INCOMPLETE" -> throw ExamDraftException.studentDataIncomplete();
            default -> throw ExamDraftException.validation(
                    "Invalid document",
                    List.of(new ExamDraftFieldError("body", "INVALID_VALUE", "The document was rejected")));
        }
    }

    private static DraftOutcomes.Item errorItem(DocumentType documentType, DocumentValidationError error) {
        String expectedName = error.details() == null ? null : error.details().expectedName();
        List<mx.uam.sapcyti.exam.domain.model.DocumentUrlFieldError> fieldErrors =
                error.details() == null ? null : error.details().fieldErrors();
        return new DraftOutcomes.Item(
                documentType,
                null,
                new DraftOutcomes.ItemError(error.error(), error.message(), expectedName, fieldErrors));
    }

    private static DraftOutcomes.Item storageItem(DocumentType documentType) {
        return new DraftOutcomes.Item(
                documentType,
                null,
                new DraftOutcomes.ItemError(STORAGE_WRITE_FAILED, STORAGE_FAILED, null, null));
    }

    private static DraftOutcomes.Item pending(DocumentType documentType) {
        return new DraftOutcomes.Item(documentType, null, null);
    }

    private static void fillSaved(List<DraftOutcomes.Item> results, ExamRequestRead saved) {
        for (int index = 0; index < results.size(); index++) {
            DraftOutcomes.Item item = results.get(index);
            if (item.error() != null || item.document() != null) {
                continue;
            }
            CurrentExamDocument document = saved.documents().stream()
                    .filter(candidate -> candidate.documentType() == item.documentType())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("stored document is missing from the draft"));
            results.set(index, new DraftOutcomes.Item(item.documentType(), document, null));
        }
    }

    private static CurrentExamDocument currentOf(ExamRequestRead saved, DocumentType documentType) {
        return saved.documents().stream()
                .filter(document -> document.documentType() == documentType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("stored document is missing from the draft"));
    }

    private static boolean changed(StudentExamContext context, Degree degree, ExamRequestBinding binding) {
        return !context.graduateProgramId().equals(binding.graduateProgramId()) || binding.degree() != degree;
    }

    private static Degree degreeOf(StudentExamContext context) {
        return switch (context.programType()) {
            case ESPECIALIZACION -> Degree.ESPECIALIZACION;
            case MAESTRIA -> Degree.MAESTRIA;
            case DOCTORADO -> Degree.DOCTORADO;
            default -> throw new IllegalStateException("degree is not supported");
        };
    }

    private static String sha256(byte[] body) {
        try {
            byte[] raw = MessageDigest.getInstance("SHA-256").digest(body);
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte value : raw) {
                hex.append(Character.forDigit((value >> 4) & 0xf, 16));
                hex.append(Character.forDigit(value & 0xf, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }

    private record Accepted(int index, DraftCommands.Document document) {
    }

    private record PreparedMutation(
            StudentExamContext context,
            ExamType examType,
            DocumentType documentType,
            int version,
            ExamRequestRead current) {
    }
}

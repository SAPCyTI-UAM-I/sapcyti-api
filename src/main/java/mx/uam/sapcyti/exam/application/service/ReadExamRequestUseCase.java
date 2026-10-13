package mx.uam.sapcyti.exam.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.academic.domain.model.StudentExamContextResult;
import mx.uam.sapcyti.academic.domain.port.out.StudentExamContextPort;
import mx.uam.sapcyti.exam.application.exception.ExamReadException;
import mx.uam.sapcyti.exam.domain.model.DegreeExamRequirementsResult;
import mx.uam.sapcyti.exam.domain.model.ExamOptions;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.service.DegreeExamCatalog;
import mx.uam.sapcyti.identity.domain.model.RoleType;

/**
 * Reads exam options and requirements for the authenticated student (SPEC-047, SPEC-049).
 *
 * <p>Identity comes from the JWT. {@code studentId}, {@code degree} and {@code examType} are not
 * taken from the body. A resolved {@code GRADO} read delegates to {@link DegreeExamCatalog} for
 * that type. A resolved doctorate {@code PREDOCTORAL} read calls the same catalog with
 * {@code PREDOCTORAL}. Neither read inserts a request.
 */
@Service
public class ReadExamRequestUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ReadExamRequestUseCase.class);

    private final StudentExamContextPort studentExamContextPort;
    private final DegreeExamCatalog degreeExamCatalog;

    public ReadExamRequestUseCase(
            StudentExamContextPort studentExamContextPort,
            DegreeExamCatalog degreeExamCatalog) {
        this.studentExamContextPort = studentExamContextPort;
        this.degreeExamCatalog = degreeExamCatalog;
    }

    @Transactional(readOnly = true)
    public ExamOptions options(Long userId, RoleType role, Long graduateProgramId) {
        return degreeExamCatalog.options(resolve(userId, role, graduateProgramId));
    }

    /**
     * Requirements for one exam type. {@code GRADO} uses that binding only. A resolved doctorate
     * {@code PREDOCTORAL} calls {@link DegreeExamCatalog} with {@code PREDOCTORAL} and does not
     * build the {@code GRADO} matrix. Specialization and master's {@code PREDOCTORAL} stay
     * {@code EXAM_TYPE_NOT_ALLOWED} before the catalog is asked to read a dossier.
     */
    @Transactional(readOnly = true)
    public ExamRequirements requirements(
            Long userId, RoleType role, Long graduateProgramId, ExamType examType) {
        StudentExamContext context = resolve(userId, role, graduateProgramId);
        if (examType == ExamType.PREDOCTORAL) {
            if (context.programType() != ProgramType.DOCTORADO) {
                throw ExamReadException.examTypeNotAllowed();
            }
            return publish(catalogRequirements(context, ExamType.PREDOCTORAL));
        }
        return publish(gradoRequirements(context));
    }

    private DegreeExamRequirementsResult gradoRequirements(StudentExamContext context) {
        try {
            return degreeExamCatalog.requirements(context);
        } catch (DataAccessException ex) {
            LOG.warn("Exam dossier could not be read", ex);
            throw ExamReadException.dependencyUnavailable();
        }
    }

    private DegreeExamRequirementsResult catalogRequirements(StudentExamContext context, ExamType examType) {
        try {
            return degreeExamCatalog.requirements(context, examType);
        } catch (DataAccessException ex) {
            LOG.warn("Exam dossier could not be read", ex);
            throw ExamReadException.dependencyUnavailable();
        }
    }

    private static ExamRequirements publish(DegreeExamRequirementsResult result) {
        if (DegreeExamRequirementsResult.ACADEMIC_PROGRAM_CHANGED.equals(result.error())) {
            throw ExamReadException.academicProgramChanged();
        }
        if (DegreeExamRequirementsResult.EXAM_TYPE_NOT_ALLOWED.equals(result.error())) {
            throw ExamReadException.examTypeNotAllowed();
        }
        return result.requirements();
    }

    private StudentExamContext resolve(Long userId, RoleType role, Long graduateProgramId) {
        if (role != RoleType.STUDENT || graduateProgramId == null) {
            throw ExamReadException.forbidden();
        }
        StudentExamContextResult result;
        try {
            result = studentExamContextPort.resolve(userId, graduateProgramId);
        } catch (DataAccessException ex) {
            LOG.warn("Student exam context could not be read", ex);
            throw ExamReadException.dependencyUnavailable();
        }
        return switch (result.outcome()) {
            case RESOLVED -> result.context();
            case NOT_FOUND -> throw ExamReadException.studentNotFound();
            case AMBIGUOUS -> throw ExamReadException.academicProgramAmbiguous();
            case INCOMPLETE -> throw ExamReadException.studentDataIncomplete();
            case DEGREE_UNSUPPORTED -> throw ExamReadException.degreeUnsupported();
            case STATUS_UNSUPPORTED -> throw ExamReadException.programStatusUnsupported();
        };
    }
}

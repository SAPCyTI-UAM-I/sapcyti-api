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
import mx.uam.sapcyti.exam.application.exception.PredoctoralCatalogNotInThisCutException;
import mx.uam.sapcyti.exam.domain.model.DegreeExamRequirementsResult;
import mx.uam.sapcyti.exam.domain.model.ExamOptions;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.service.DegreeExamCatalog;
import mx.uam.sapcyti.identity.domain.model.RoleType;

/**
 * Reads exam options and GRADO requirements for the authenticated student (SPEC-047).
 *
 * <p>Identity comes from the JWT. {@code studentId}, {@code degree} and {@code status} are not
 * taken from the request. A resolved {@code GRADO} read delegates to {@link DegreeExamCatalog}.
 * {@code PREDOCTORAL} requirements are not that catalog.
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
     * Requirements for one exam type. {@code GRADO} uses the degree catalog. Specialization and
     * master's {@code PREDOCTORAL} is refused. A doctorate {@code PREDOCTORAL} does not call the
     * GRADO catalog; HU-63 replaces that branch.
     */
    @Transactional(readOnly = true)
    public ExamRequirements requirements(
            Long userId, RoleType role, Long graduateProgramId, ExamType examType) {
        StudentExamContext context = resolve(userId, role, graduateProgramId);
        if (examType == ExamType.PREDOCTORAL) {
            if (context.programType() != ProgramType.DOCTORADO) {
                throw ExamReadException.examTypeNotAllowed();
            }
            throw new PredoctoralCatalogNotInThisCutException();
        }
        return gradoRequirements(context);
    }

    private ExamRequirements gradoRequirements(StudentExamContext context) {
        DegreeExamRequirementsResult result;
        try {
            result = degreeExamCatalog.requirements(context);
        } catch (DataAccessException ex) {
            LOG.warn("Exam dossier could not be read", ex);
            throw ExamReadException.dependencyUnavailable();
        }
        if (DegreeExamRequirementsResult.ACADEMIC_PROGRAM_CHANGED.equals(result.error())) {
            throw ExamReadException.academicProgramChanged();
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

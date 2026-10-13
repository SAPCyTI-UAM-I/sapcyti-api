package mx.uam.sapcyti.exam.domain.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.DegreeExamRequirementsResult;
import mx.uam.sapcyti.exam.domain.model.DocumentKind;
import mx.uam.sapcyti.exam.domain.model.DocumentKindAdmission;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.EligibilityReason;
import mx.uam.sapcyti.exam.domain.model.ExamOptions;
import mx.uam.sapcyti.exam.domain.model.ExamRequestBinding;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamRequestSummary;
import mx.uam.sapcyti.exam.domain.model.ExamRequirement;
import mx.uam.sapcyti.exam.domain.model.ExamRequirements;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.GradoRequirementRule;
import mx.uam.sapcyti.exam.domain.model.OfficialDocumentName;
import mx.uam.sapcyti.exam.domain.model.PredoctoralRequirementRule;
import mx.uam.sapcyti.exam.domain.model.ProgramStatus;
import mx.uam.sapcyti.exam.domain.model.RequirementStatus;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import org.springframework.stereotype.Service;

/**
 * Derived catalog for one resolved {@link StudentExamContext} (SPEC-046, SPEC-048).
 *
 * <p>Options, official names and document status are computed. They are not stored on the request.
 * The read port is not asked to create a dossier. {@code PREDOCTORAL} uses the doctoral list and
 * an optional article. A context that is not resolved never reaches this service.
 */
@Service
@RequiredArgsConstructor
public class DegreeExamCatalog {

    public static final String PROGRAM_NOT_ELIGIBLE = "PROGRAM_NOT_ELIGIBLE";

    private final ExamRequestReadPort examRequestReadPort;

    public ExamOptions options(StudentExamContext context) {
        StudentExamContext resolved = requireContext(context);
        Degree degree = toDegree(resolved.programType());
        Eligibility eligibility = eligibility(resolved.programStatus());
        return new ExamOptions(
                degree,
                toStatus(resolved.programStatus()),
                eligibility.eligible(),
                eligibility.reason(),
                examTypes(degree));
    }

    /**
     * GRADO requirements for the resolved context. A binding whose program or degree differs
     * yields {@code ACADEMIC_PROGRAM_CHANGED} and does not open another request of this type.
     */
    public DegreeExamRequirementsResult requirements(StudentExamContext context) {
        return requirements(context, ExamType.GRADO);
    }

    /**
     * Requirements for the requested exam type. Current documents come from that type's binding.
     * {@code PREDOCTORAL} for a degree other than doctorate is {@code EXAM_TYPE_NOT_ALLOWED}
     * and does not read or insert a dossier. Official names are not recomputed by a new algorithm.
     */
    public DegreeExamRequirementsResult requirements(StudentExamContext context, ExamType examType) {
        StudentExamContext resolved = requireContext(context);
        Objects.requireNonNull(examType, "examType");
        Degree degree = toDegree(resolved.programType());
        if (examType == ExamType.PREDOCTORAL && degree != Degree.DOCTORADO) {
            return DegreeExamRequirementsResult.examTypeNotAllowed();
        }
        Optional<ExamRequestRead> dossier = examRequestReadPort.findByStudentAndType(
                resolved.studentId(), examType);
        if (dossier.isPresent() && mismatched(resolved, degree, dossier.get().binding())) {
            return DegreeExamRequirementsResult.academicProgramChanged();
        }
        Eligibility eligibility = eligibility(resolved.programStatus());
        Map<DocumentType, CurrentExamDocument> current = dossier.map(ExamRequestRead::documents)
                .map(DegreeExamCatalog::index)
                .orElseGet(Map::of);
        List<ExamRequirement> requirements = rules(examType, degree).stream()
                .map(rule -> requirement(rule, resolved, degree, current))
                .toList();
        ExamRequestSummary summary = dossier.map(read -> summary(read.binding())).orElse(null);
        return DegreeExamRequirementsResult.ready(new ExamRequirements(
                examType,
                degree,
                toStatus(resolved.programStatus()),
                eligibility.eligible(),
                eligibility.reason(),
                summary,
                requirements));
    }

    /**
     * Later create, upload or submit consult this predicate. {@code BAJA} and {@code SUSPENSION}
     * yield {@code PROGRAM_NOT_ELIGIBLE}. The read port is not consulted and the dossier is unchanged.
     */
    public Optional<String> writeRejection(StudentExamContext context) {
        StudentExamContext resolved = requireContext(context);
        return switch (resolved.programStatus()) {
            case BAJA, SUSPENSION -> Optional.of(PROGRAM_NOT_ELIGIBLE);
            case EN_INVESTIGACION, EGRESADO -> Optional.empty();
        };
    }

    /**
     * Rule of one document type for the resolved degree and exam type.
     * Does not read the dossier. Empty when that matrix does not list the type,
     * including a predoctoral exam for a degree other than doctorate.
     */
    public Optional<DocumentRule> documentRule(
            StudentExamContext context, ExamType examType, DocumentType documentType) {
        StudentExamContext resolved = requireContext(context);
        Objects.requireNonNull(examType, "examType");
        Objects.requireNonNull(documentType, "documentType");
        return documentRules(examType, toDegree(resolved.programType())).stream()
                .filter(rule -> rule.documentType() == documentType)
                .findFirst();
    }

    /**
     * Whether the matrix admits this type and kind. Does not read the dossier.
     * A type that belongs to another degree is not admitted.
     */
    public DocumentKindAdmission kindAdmission(
            StudentExamContext context, ExamType examType, DocumentType documentType, DocumentKind kind) {
        StudentExamContext resolved = requireContext(context);
        Objects.requireNonNull(examType, "examType");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(kind, "kind");
        Degree degree = toDegree(resolved.programType());
        boolean admitted = documentRules(examType, degree).stream()
                .anyMatch(rule -> rule.documentType() == documentType && rule.acceptedKinds().contains(kind));
        return new DocumentKindAdmission(examType, degree, documentType, kind, admitted);
    }

    /**
     * Official file name for one type. {@code ARTICULO} has a null file name.
     * A missing given name or paternal surname fails and no name is invented.
     */
    public OfficialDocumentName officialName(StudentExamContext context, DocumentType documentType) {
        StudentExamContext resolved = requireContext(context);
        Objects.requireNonNull(documentType, "documentType");
        return OfficialDocumentName.of(
                documentType,
                toDegree(resolved.programType()),
                resolved.firstName(),
                resolved.firstLastName(),
                resolved.secondLastName());
    }

    private static List<CatalogRule> rules(ExamType examType, Degree degree) {
        return documentRules(examType, degree).stream()
                .map(rule -> new CatalogRule(rule.documentType(), rule.required(), rule.acceptedKinds()))
                .toList();
    }

    private static List<DocumentRule> documentRules(ExamType examType, Degree degree) {
        if (examType == ExamType.PREDOCTORAL) {
            if (degree != Degree.DOCTORADO) {
                return List.of();
            }
            return PredoctoralRequirementRule.doctoral().stream().map(DegreeExamCatalog::toRule).toList();
        }
        return GradoRequirementRule.forDegree(degree).stream().map(DegreeExamCatalog::toRule).toList();
    }

    private static DocumentRule toRule(GradoRequirementRule rule) {
        return new DocumentRule(
                rule.documentType(), rule.required(), rule.acceptedKinds(), rule.namePrescribed());
    }

    private static DocumentRule toRule(PredoctoralRequirementRule rule) {
        return new DocumentRule(
                rule.documentType(), rule.required(), rule.acceptedKinds(), rule.namePrescribed());
    }

    private static ExamRequirement requirement(
            CatalogRule rule,
            StudentExamContext context,
            Degree degree,
            Map<DocumentType, CurrentExamDocument> current) {
        String fileName = OfficialDocumentName.of(
                rule.documentType(),
                degree,
                context.firstName(),
                context.firstLastName(),
                context.secondLastName()).fileName();
        CurrentExamDocument document = current.get(rule.documentType());
        RequirementStatus status = document == null ? RequirementStatus.PENDIENTE : RequirementStatus.CARGADO;
        return new ExamRequirement(
                rule.documentType(),
                rule.required(),
                rule.acceptedKinds(),
                fileName,
                fileName,
                status,
                document);
    }

    private static ExamRequestSummary summary(ExamRequestBinding binding) {
        return new ExamRequestSummary(
                binding.id(),
                binding.examType(),
                binding.degree(),
                binding.status(),
                binding.folio(),
                binding.version(),
                binding.submittedAt());
    }

    private static boolean mismatched(StudentExamContext context, Degree degree, ExamRequestBinding binding) {
        return !context.graduateProgramId().equals(binding.graduateProgramId()) || binding.degree() != degree;
    }

    private static Map<DocumentType, CurrentExamDocument> index(List<CurrentExamDocument> documents) {
        Map<DocumentType, CurrentExamDocument> indexed = new EnumMap<>(DocumentType.class);
        for (CurrentExamDocument document : documents) {
            indexed.putIfAbsent(document.documentType(), document);
        }
        return indexed;
    }

    private static List<ExamType> examTypes(Degree degree) {
        if (degree == Degree.DOCTORADO) {
            return List.of(ExamType.GRADO, ExamType.PREDOCTORAL);
        }
        return List.of(ExamType.GRADO);
    }

    private static Eligibility eligibility(mx.uam.sapcyti.academic.domain.model.ProgramStatus status) {
        return switch (status) {
            case EN_INVESTIGACION, EGRESADO -> new Eligibility(true, null);
            case BAJA -> new Eligibility(false, EligibilityReason.PROGRAM_BAJA);
            case SUSPENSION -> new Eligibility(false, EligibilityReason.PROGRAM_SUSPENSION);
        };
    }

    private static Degree toDegree(mx.uam.sapcyti.academic.domain.model.ProgramType programType) {
        return switch (programType) {
            case ESPECIALIZACION -> Degree.ESPECIALIZACION;
            case MAESTRIA -> Degree.MAESTRIA;
            case DOCTORADO -> Degree.DOCTORADO;
        };
    }

    private static ProgramStatus toStatus(mx.uam.sapcyti.academic.domain.model.ProgramStatus status) {
        return switch (status) {
            case EN_INVESTIGACION -> ProgramStatus.EN_INVESTIGACION;
            case EGRESADO -> ProgramStatus.EGRESADO;
            case BAJA -> ProgramStatus.BAJA;
            case SUSPENSION -> ProgramStatus.SUSPENSION;
        };
    }

    private static StudentExamContext requireContext(StudentExamContext context) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(context.studentId(), "studentId");
        Objects.requireNonNull(context.graduateProgramId(), "graduateProgramId");
        Objects.requireNonNull(context.programType(), "programType");
        Objects.requireNonNull(context.programStatus(), "programStatus");
        return context;
    }

    private record Eligibility(boolean eligible, EligibilityReason reason) {
    }

    private record CatalogRule(DocumentType documentType, boolean required, List<DocumentKind> acceptedKinds) {
    }

    /**
     * One catalog row used to admit a document. {@code required} does not change admission.
     * {@code namePrescribed} is false only for {@code ARTICULO}.
     */
    public record DocumentRule(
            DocumentType documentType,
            boolean required,
            List<DocumentKind> acceptedKinds,
            boolean namePrescribed) {

        public DocumentRule {
            Objects.requireNonNull(documentType, "documentType");
            Objects.requireNonNull(acceptedKinds, "acceptedKinds");
            acceptedKinds = List.copyOf(acceptedKinds);
        }
    }
}

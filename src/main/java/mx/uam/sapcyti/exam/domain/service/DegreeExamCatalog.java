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
import mx.uam.sapcyti.exam.domain.model.ProgramStatus;
import mx.uam.sapcyti.exam.domain.model.RequirementStatus;
import mx.uam.sapcyti.exam.domain.port.out.ExamRequestReadPort;
import org.springframework.stereotype.Service;

/**
 * Derived GRADO catalog for one resolved {@link StudentExamContext} (SPEC-046).
 *
 * <p>Options, official names and document status are computed. They are not stored on the request.
 * The read port is not asked to create a dossier. {@code PREDOCTORAL} requirements are HU-63.
 * A context that is not resolved never reaches this service.
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
        StudentExamContext resolved = requireContext(context);
        Degree degree = toDegree(resolved.programType());
        Optional<ExamRequestRead> dossier = examRequestReadPort.findByStudentAndType(
                resolved.studentId(), ExamType.GRADO);
        if (dossier.isPresent() && mismatched(resolved, degree, dossier.get().binding())) {
            return DegreeExamRequirementsResult.academicProgramChanged();
        }
        Eligibility eligibility = eligibility(resolved.programStatus());
        Map<DocumentType, CurrentExamDocument> current = dossier.map(ExamRequestRead::documents)
                .map(DegreeExamCatalog::index)
                .orElseGet(Map::of);
        List<ExamRequirement> requirements = GradoRequirementRule.forDegree(degree).stream()
                .map(rule -> requirement(rule, resolved, degree, current))
                .toList();
        ExamRequestSummary summary = dossier.map(read -> summary(read.binding())).orElse(null);
        return DegreeExamRequirementsResult.ready(new ExamRequirements(
                ExamType.GRADO,
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

    private static ExamRequirement requirement(
            GradoRequirementRule rule,
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
}

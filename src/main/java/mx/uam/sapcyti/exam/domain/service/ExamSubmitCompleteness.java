package mx.uam.sapcyti.exam.domain.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.exam.domain.model.CurrentExamDocument;
import mx.uam.sapcyti.exam.domain.model.Degree;
import mx.uam.sapcyti.exam.domain.model.DocumentType;
import mx.uam.sapcyti.exam.domain.model.ExamRequestRead;
import mx.uam.sapcyti.exam.domain.model.ExamType;
import mx.uam.sapcyti.exam.domain.model.GradoRequirementRule;
import mx.uam.sapcyti.exam.domain.model.PredoctoralRequirementRule;
import mx.uam.sapcyti.exam.domain.model.SubmitCompleteness;
import mx.uam.sapcyti.exam.domain.model.SubmitCompletenessResult;
import org.springframework.stereotype.Service;

/**
 * Predicate a later submit consults (SPEC-048). It does not change status, folio or delivery
 * intentions, and it does not insert a binding or a stored object.
 *
 * <p>The {@code BAJA}/{@code SUSPENSION} predicate of SPEC-046 is evaluated first. The use case
 * passes the resolved context and the dossier it already loaded.
 */
@Service
@RequiredArgsConstructor
public class ExamSubmitCompleteness {

    private final DegreeExamCatalog degreeExamCatalog;

    public SubmitCompletenessResult evaluate(
            StudentExamContext context, ExamType examType, ExamRequestRead dossier) {
        Objects.requireNonNull(examType, "examType");
        Optional<String> rejection = degreeExamCatalog.writeRejection(context);
        if (rejection.isPresent()) {
            return SubmitCompletenessResult.programNotEligible();
        }
        Degree degree = toDegree(context.programType());
        if (examType == ExamType.PREDOCTORAL && degree != Degree.DOCTORADO) {
            return SubmitCompletenessResult.examTypeNotAllowed();
        }
        Set<DocumentType> loaded = loaded(dossier);
        List<DocumentType> missing = required(examType, degree).stream()
                .filter(type -> !loaded.contains(type))
                .toList();
        SubmitCompleteness completeness = new SubmitCompleteness(examType, degree, missing);
        if (missing.isEmpty()) {
            return SubmitCompletenessResult.complete(completeness);
        }
        return SubmitCompletenessResult.missing(completeness);
    }

    private static Set<DocumentType> loaded(ExamRequestRead dossier) {
        Set<DocumentType> loaded = EnumSet.noneOf(DocumentType.class);
        if (dossier == null) {
            return loaded;
        }
        for (CurrentExamDocument document : dossier.documents()) {
            loaded.add(document.documentType());
        }
        return loaded;
    }

    private static List<DocumentType> required(ExamType examType, Degree degree) {
        if (examType == ExamType.PREDOCTORAL) {
            return PredoctoralRequirementRule.doctoral().stream()
                    .filter(PredoctoralRequirementRule::required)
                    .map(PredoctoralRequirementRule::documentType)
                    .toList();
        }
        return GradoRequirementRule.forDegree(degree).stream()
                .filter(GradoRequirementRule::required)
                .map(GradoRequirementRule::documentType)
                .toList();
    }

    private static Degree toDegree(mx.uam.sapcyti.academic.domain.model.ProgramType programType) {
        return switch (programType) {
            case ESPECIALIZACION -> Degree.ESPECIALIZACION;
            case MAESTRIA -> Degree.MAESTRIA;
            case DOCTORADO -> Degree.DOCTORADO;
        };
    }
}

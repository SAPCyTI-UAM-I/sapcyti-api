package mx.uam.sapcyti.exam.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * One derived GRADO rule (exam-requests.schema.json#/definitions/GradoRequirementRule).
 * The matrix is computed from the degree. It is not copied onto each request.
 */
public record GradoRequirementRule(
        DocumentType documentType,
        boolean required,
        List<DocumentKind> acceptedKinds,
        boolean namePrescribed) {

    private static final List<DocumentType> NON_DOCTORAL = List.of(
            DocumentType.TESIS,
            DocumentType.SOL_SINODALES,
            DocumentType.HIST_ACAD,
            DocumentType.COMP_IDIOMA,
            DocumentType.DATOS_ALUM);

    private static final List<DocumentType> DOCTORAL = List.of(
            DocumentType.TESIS,
            DocumentType.ARTICULO,
            DocumentType.SOL_SINODALES,
            DocumentType.HIST_ACAD,
            DocumentType.COMP_IDIOMA,
            DocumentType.DATOS_ALUM);

    public GradoRequirementRule {
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(acceptedKinds, "acceptedKinds");
        if (acceptedKinds.isEmpty() || new HashSet<>(acceptedKinds).size() != acceptedKinds.size()) {
            throw new IllegalArgumentException("acceptedKinds must be unique and non-empty");
        }
        acceptedKinds = List.copyOf(acceptedKinds);
        boolean article = documentType == DocumentType.ARTICULO;
        if (article == namePrescribed) {
            throw new IllegalArgumentException("namePrescribed is false only for ARTICULO");
        }
    }

    public static List<GradoRequirementRule> forDegree(Degree degree) {
        Objects.requireNonNull(degree, "degree");
        List<DocumentType> types = degree == Degree.DOCTORADO ? DOCTORAL : NON_DOCTORAL;
        return types.stream().map(GradoRequirementRule::rule).toList();
    }

    private static GradoRequirementRule rule(DocumentType documentType) {
        if (documentType == DocumentType.ARTICULO) {
            return new GradoRequirementRule(
                    documentType, true, List.of(DocumentKind.PDF, DocumentKind.URL), false);
        }
        return new GradoRequirementRule(documentType, true, List.of(DocumentKind.PDF), true);
    }
}

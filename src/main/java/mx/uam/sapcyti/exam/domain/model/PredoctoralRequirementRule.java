package mx.uam.sapcyti.exam.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * One derived PREDOCTORAL rule (exam-requests.schema.json#/definitions/PredoctoralRequirementRule).
 * Published only for a doctoral degree. The matrix is computed and is not copied onto the request.
 * {@code ARTICULO} is optional; the thesis token stays {@code Docto}.
 */
public record PredoctoralRequirementRule(
        DocumentType documentType,
        boolean required,
        List<DocumentKind> acceptedKinds,
        boolean namePrescribed) {

    private static final List<DocumentType> DOCTORAL = List.of(
            DocumentType.TESIS,
            DocumentType.ARTICULO,
            DocumentType.SOL_SINODALES,
            DocumentType.HIST_ACAD,
            DocumentType.COMP_IDIOMA,
            DocumentType.DATOS_ALUM);

    public PredoctoralRequirementRule {
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
        if (article == required) {
            throw new IllegalArgumentException("ARTICULO is optional only on PREDOCTORAL");
        }
    }

    public static List<PredoctoralRequirementRule> doctoral() {
        return DOCTORAL.stream().map(PredoctoralRequirementRule::rule).toList();
    }

    private static PredoctoralRequirementRule rule(DocumentType documentType) {
        if (documentType == DocumentType.ARTICULO) {
            return new PredoctoralRequirementRule(
                    documentType, false, List.of(DocumentKind.PDF, DocumentKind.URL), false);
        }
        return new PredoctoralRequirementRule(documentType, true, List.of(DocumentKind.PDF), true);
    }
}

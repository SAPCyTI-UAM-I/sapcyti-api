package mx.uam.sapcyti.exam.domain.model;

import java.text.Normalizer;
import java.util.Objects;

/**
 * Canonical PDF file name for one prescribed document and one student
 * (exam-requests.schema.json#/definitions/OfficialDocumentName).
 * {@code expectedName} and {@code exampleName} are this same string.
 * {@code ARTICULO} has a null file name.
 */
public record OfficialDocumentName(DocumentType documentType, String fileName) {

    public OfficialDocumentName {
        Objects.requireNonNull(documentType, "documentType");
        if (documentType == DocumentType.ARTICULO) {
            if (fileName != null) {
                throw new IllegalArgumentException("ARTICULO has no file name");
            }
        } else if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("file name is required");
        }
    }

    /**
     * Builds the canonical name from official name parts. A missing given name or paternal surname
     * fails the read; a null or blank maternal surname is omitted. Letter case is kept.
     */
    public static OfficialDocumentName of(
            DocumentType documentType,
            Degree degree,
            String firstName,
            String firstLastName,
            String secondLastName) {
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(degree, "degree");
        if (documentType == DocumentType.ARTICULO) {
            return new OfficialDocumentName(documentType, null);
        }
        String paternal = requiredPart(firstLastName);
        String given = requiredPart(firstName);
        String maternal = optionalPart(secondLastName);
        String stem = maternal == null ? paternal + "_" + given : paternal + "_" + maternal + "_" + given;
        return new OfficialDocumentName(documentType, prefix(documentType, degree) + "_" + stem + ".pdf");
    }

    private static String prefix(DocumentType documentType, Degree degree) {
        return switch (documentType) {
            case TESIS -> "Tesis_" + DegreeNameToken.forDegree(degree).name();
            case SOL_SINODALES -> "Sol_Sinodales";
            case HIST_ACAD -> "Hist_Acad";
            case COMP_IDIOMA -> "Comp_Idioma";
            case DATOS_ALUM -> "Datos_Alum";
            case ARTICULO -> throw new IllegalArgumentException("ARTICULO has no file name");
        };
    }

    private static String requiredPart(String part) {
        if (part == null || part.isBlank()) {
            throw new IllegalArgumentException("Official name is incomplete");
        }
        String normalized = normalize(part);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Official name is incomplete");
        }
        return normalized;
    }

    private static String optionalPart(String part) {
        if (part == null || part.isBlank()) {
            return null;
        }
        String normalized = normalize(part);
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Trims, maps ñ/Ñ, strips Unicode diacritics, and collapses internal whitespace to one underscore.
     */
    private static String normalize(String part) {
        String withoutEnie = part.trim().replace("ñ", "n").replace("Ñ", "N");
        String stripped = Normalizer.normalize(withoutEnie, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return stripped.replaceAll("\\s+", "_");
    }
}
